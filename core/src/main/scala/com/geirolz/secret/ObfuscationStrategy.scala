package com.geirolz.secret

import com.geirolz.secret.{KeyBuffer, ObfuscatedValueBuffer, PlainValueBuffer}
import com.geirolz.secret.internal.BytesUtils.{clearByteArray, clearByteBuffer}
import com.geirolz.secret.ObfuscationStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.internal.KeyValueBuffer
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.SecureRandom

opaque type ObfuscationStrategy[P] = (Obfuscator[P], DeObfuscator[P])
object ObfuscationStrategy extends ObfuscationStrategyBuilders with DefaultObfuscationStrategyInstances:

  extension [P](obfuscationStrategy: ObfuscationStrategy[P])
    def obfuscator: Obfuscator[P]     = obfuscationStrategy._1
    def deObfuscator: DeObfuscator[P] = obfuscationStrategy._2
    def bimap[U](fO: U => P, fD: P => U): ObfuscationStrategy[U] =
      ObfuscationStrategy.of(
        obfuscator   = Obfuscator(plain => obfuscator(fO(plain))),
        deObfuscator = DeObfuscator(bufferTuple => fD(deObfuscator(bufferTuple)))
      )

  def apply[P: ObfuscationStrategy]: ObfuscationStrategy[P] = summon[ObfuscationStrategy[P]]

  def of[P](obfuscator: Obfuscator[P], deObfuscator: DeObfuscator[P]): ObfuscationStrategy[P] =
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

private[secret] sealed trait ObfuscationStrategyBuilders:

  def withDefaultDirectByteBuffer[P](capacity: Int)(
    fillBuffer: ByteBuffer => P => PlainValueBuffer,
    readBuffer: PlainValueBuffer => P
  ): ObfuscationStrategy[P] =
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
  ): ObfuscationStrategy[P] =
    ObfuscationStrategy.of(
      obfuscator   = buildObfuscator((plainValue: P) => fillBuffer(ByteBuffer.allocateDirect(capacity)).apply(plainValue)),
      deObfuscator = buildDeObfuscator((buffer: PlainValueBuffer) => readBuffer(buffer.rewind().asReadOnlyBuffer()))
    )

  def defaultForString(charset: Charset): ObfuscationStrategy[String] =
    summon[ObfuscationStrategy[Array[Byte]]].bimap(_.getBytes(charset), new String(_, charset))

private[secret] trait DefaultObfuscationStrategyInstances:

  given ObfuscationStrategy[Array[Byte]] =
    ObfuscationStrategy.of[Array[Byte]](
      obfuscator = Obfuscator.default((plainBytes: Array[Byte]) => ByteBuffer.allocateDirect(plainBytes.length).put(plainBytes)),
      deObfuscator = DeObfuscator.default((plainBuffer: PlainValueBuffer) => {
        val result = new Array[Byte](plainBuffer.capacity())
        plainBuffer.rewind().get(result)
        result
      })
    )

  given ObfuscationStrategy[String] =
    ObfuscationStrategy.defaultForString(Charset.defaultCharset())

  given ObfuscationStrategy[Byte] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(1)(_.put, _.get)

  given ObfuscationStrategy[Char] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(2)(_.putChar, _.getChar)

  given ObfuscationStrategy[Short] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(2)(_.putShort, _.getShort)

  given ObfuscationStrategy[Int] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(4)(_.putInt, _.getInt)

  given ObfuscationStrategy[Long] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(8)(_.putLong, _.getLong)

  given ObfuscationStrategy[Float] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(4)(_.putFloat, _.getFloat)

  given ObfuscationStrategy[Double] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(8)(_.putDouble, _.getDouble)

  given ObfuscationStrategy[Boolean] =
    ObfuscationStrategy.withDefaultDirectByteBuffer(1)(
      (b: PlainValueBuffer) => (v: Boolean) => b.put(if (v) 1.toByte else 0.toByte),
      _.get == 1.toByte
    )

  given ObfuscationStrategy[BigInt] =
    summon[ObfuscationStrategy[Array[Byte]]].bimap(_.toByteArray, BigInt(_))

  given ObfuscationStrategy[BigDecimal] =
    summon[ObfuscationStrategy[String]].bimap(_.toString, str => BigDecimal(str))
