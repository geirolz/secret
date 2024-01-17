package com.geirolz.secret

import com.geirolz.secret.Secret.secretStrategyForBytes
import com.geirolz.secret.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.internal.BytesUtils.{clearByteArray, clearByteBuffer}
import com.geirolz.secret.{KeyBuffer, ObfuscatedValueBuffer, PlainValueBuffer}

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.SecureRandom
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

opaque type SecretStrategy[P] = (Obfuscator[P], DeObfuscator[P])
object SecretStrategy extends SecretStrategyBuilders with DefaultSecretStrategyInstances:

  extension [P](strategy: SecretStrategy[P])
    def obfuscator: Obfuscator[P]     = strategy._1
    def deObfuscator: DeObfuscator[P] = strategy._2
    def bimap[U](fO: U => P, fD: P => U): SecretStrategy[U] =
      SecretStrategy(
        obfuscator   = Obfuscator(plain => obfuscator(fO(plain))),
        deObfuscator = DeObfuscator(bufferTuple => fD(deObfuscator(bufferTuple)))
      )

  def apply[P: SecretStrategy]: SecretStrategy[P] = summon[SecretStrategy[P]]

  def apply[P](obfuscator: Obfuscator[P], deObfuscator: DeObfuscator[P]): SecretStrategy[P] =
    (obfuscator, deObfuscator)

  // ------------------ Obfuscator ------------------
  opaque type Obfuscator[P] = P => KeyValueBuffer
  object Obfuscator:

    extension [P](obfuscator: Obfuscator[P])
      /** Obfuscate a plain value. */
      def apply(plain: P): KeyValueBuffer = obfuscator(plain)

    /** Create a new Obfuscator from a function. */
    def apply[P](f: P => KeyValueBuffer): Obfuscator[P] = f

    /** Create a new Obfuscator which obfuscate value using a Xor formula.
      *
      * Formula: `plainValue[i] ^ (key[len - i] ^ (len * i))`
      *
      * Example:
      * {{{
      *   //Index      =    1     2     3     4    5
      *   //Plain      = [0x01][0x02][0x03][0x04][0x05]
      *   //Key        = [0x9d][0x10][0xad][0x87][0x2b]
      *   //Obfuscated = [0x9c][0x12][0xae][0x83][0x2e]
      * }}}
      */
    def default[P](f: P => PlainValueBuffer): Obfuscator[P] = {

      def genKeyBuffer(secureRandom: SecureRandom, size: Int): KeyBuffer =
        val keyBuffer = ByteBuffer.allocateDirect(size)
        var keyArray  = new Array[Byte](size)
        secureRandom.nextBytes(keyArray)
        keyBuffer.put(keyArray)
        keyArray = clearByteArray(keyArray)
        keyBuffer

      (plain: P) =>
        val secureRandom: SecureRandom         = new SecureRandom()
        var plainBuffer: PlainValueBuffer      = f(plain)
        val capacity: Int                      = plainBuffer.capacity()
        val keyBuffer: KeyBuffer               = genKeyBuffer(secureRandom, capacity)
        val valueBuffer: ObfuscatedValueBuffer = ByteBuffer.allocateDirect(capacity)
        for (i <- 0 until capacity)
          valueBuffer.put(
            (plainBuffer.get(i) ^ (keyBuffer.get(capacity - 1 - i) ^ (capacity * i).toByte)).toByte
          )

        // clear plainBuffer
        plainBuffer = clearByteBuffer(plainBuffer)

        new KeyValueBuffer(keyBuffer, valueBuffer)
    }

  // ------------------ DeObfuscator ------------------
  opaque type DeObfuscator[P] = KeyValueBuffer => P
  object DeObfuscator:

    extension [P](obfuscator: DeObfuscator[P])
      /** DeObfuscate an obfuscated value. */
      def apply(bufferTuple: KeyValueBuffer): P = obfuscator(bufferTuple)

    /** Create a new DeObfuscator from a function. */
    def apply[P](f: KeyValueBuffer => P): DeObfuscator[P] = f

    /** Create a new DeObfuscator which de-obfuscate value using a Xor formula.
      *
      * Formula: `obfuscated[i] ^ (key[len - i] ^ (len * i))`
      *
      * Example:
      * {{{
      *   //Index      =    1     2     3     4    5
      *   //Obfuscated = [0x9c][0x12][0xae][0x83][0x2e]
      *   //Key        = [0x9d][0x10][0xad][0x87][0x2b]
      *   //Plain      = [0x01][0x02][0x03][0x04][0x05]
      * }}}
      */
    def default[P](f: PlainValueBuffer => P): DeObfuscator[P] =
      bufferTuple =>
        val capacity: Int                      = bufferTuple.roKeyBuffer.capacity()
        var plainValueBuffer: PlainValueBuffer = ByteBuffer.allocateDirect(capacity)

        for (i <- 0 until capacity)
          plainValueBuffer.put(
            (bufferTuple.roObfuscatedBuffer.get(i) ^ (bufferTuple.roKeyBuffer.get(capacity - 1 - i) ^ (capacity * i).toByte)).toByte
          )

        val result = f(plainValueBuffer.asReadOnlyBuffer())

        // clear plainValueBuffer
        plainValueBuffer = clearByteBuffer(plainValueBuffer)

        result

private[secret] sealed trait SecretStrategyBuilders:

  def withDefaultDirectByteBuffer[P](capacity: Int)(
    fillBuffer: ByteBuffer => P => PlainValueBuffer,
    readBuffer: PlainValueBuffer => P
  ): SecretStrategy[P] =
    withDirectByteBuffer(capacity)(
      buildObfuscator   = Obfuscator.default,
      buildDeObfuscator = DeObfuscator.default,
      fillBuffer        = fillBuffer,
      readBuffer        = readBuffer
    )

  def withDirectByteBuffer[P](capacity: Int)(
    buildObfuscator: (P => PlainValueBuffer) => Obfuscator[P],
    buildDeObfuscator: (PlainValueBuffer => P) => DeObfuscator[P],
    fillBuffer: ByteBuffer => P => PlainValueBuffer,
    readBuffer: PlainValueBuffer => P
  ): SecretStrategy[P] =
    SecretStrategy(
      obfuscator   = buildObfuscator((plainValue: P) => fillBuffer(ByteBuffer.allocateDirect(capacity)).apply(plainValue)),
      deObfuscator = buildDeObfuscator((buffer: PlainValueBuffer) => readBuffer(buffer.rewind().asReadOnlyBuffer()))
    )

  def defaultForString(charset: Charset): SecretStrategy[String] =
    secretStrategyForBytes.bimap(_.getBytes(charset), new String(_, charset))

private[secret] trait DefaultSecretStrategyInstances:

  given SecretStrategy[Short] =
    SecretStrategy.withDefaultDirectByteBuffer(2)(_.putShort, _.getShort)

  given SecretStrategy[Int] =
    SecretStrategy.withDefaultDirectByteBuffer(4)(_.putInt, _.getInt)

  given SecretStrategy[Long] =
    SecretStrategy.withDefaultDirectByteBuffer(8)(_.putLong, _.getLong)

  given SecretStrategy[Float] =
    SecretStrategy.withDefaultDirectByteBuffer(4)(_.putFloat, _.getFloat)

  given SecretStrategy[Double] =
    SecretStrategy.withDefaultDirectByteBuffer(8)(_.putDouble, _.getDouble)

  given SecretStrategy[BigInt] =
    secretStrategyForBytes.bimap(_.toByteArray, BigInt(_))

  given SecretStrategy[BigDecimal] =
    summon[SecretStrategy[String]].bimap(_.toString, str => BigDecimal(str))

  // other
  given SecretStrategy[String] =
    SecretStrategy.defaultForString(Charset.defaultCharset())

  given SecretStrategy[Boolean] =
    SecretStrategy.withDefaultDirectByteBuffer(1)(
      fillBuffer = (b: PlainValueBuffer) => (v: Boolean) => b.put(if (v) 1.toByte else 0.toByte),
      readBuffer = _.get == 1.toByte
    )

  given SecretStrategy[Byte] =
    SecretStrategy.withDefaultDirectByteBuffer(1)(_.put, _.get)

  given SecretStrategy[Char] =
    SecretStrategy.withDefaultDirectByteBuffer(2)(_.putChar, _.getChar)

  // collections
  given secretStrategyForBytes: SecretStrategy[Array[Byte]] =
    SecretStrategy[Array[Byte]](
      obfuscator = Obfuscator.default((plainBytes: Array[Byte]) => ByteBuffer.allocateDirect(plainBytes.length).put(plainBytes)),
      deObfuscator = DeObfuscator.default((plainBuffer: PlainValueBuffer) => {
        val result = new Array[Byte](plainBuffer.capacity())
        plainBuffer.rewind().get(result)
        result
      })
    )

  given secretStrategyForChars: SecretStrategy[Array[Char]] =
    summon[SecretStrategy[String]].bimap(new String(_), _.toCharArray)

  given [T: ClassTag](using ss: SecretStrategy[Array[T]]): SecretStrategy[ArraySeq[T]] =
    ss.bimap(_.toArray, ArraySeq.from)
