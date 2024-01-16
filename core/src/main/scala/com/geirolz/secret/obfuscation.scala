package com.geirolz.secret

import com.geirolz.secret.{KeyBuffer, ObfuscatedValueBuffer, PlainValueBuffer}
import com.geirolz.secret.internal.BytesUtils.{clearByteArray, clearByteBuffer}
import com.geirolz.secret.internal.KeyValueBuffer

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.SecureRandom

trait Obfuscator[P] extends (P => KeyValueBuffer)
object Obfuscator:

  def apply[P: Obfuscator]: Obfuscator[P] =
    summon[Obfuscator[P]]

  /** Create a new Obfuscator which obfuscate value using a custom formula.
    *
    * @param f
    *   the function which obfuscate the value
    */
  def of[P](f: P => KeyValueBuffer): Obfuscator[P] = f(_)

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

    of { (plain: P) =>
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
  }

trait DeObfuscator[P] extends (KeyValueBuffer => P)
object DeObfuscator:

  def apply[P: DeObfuscator]: DeObfuscator[P] =
    summon[DeObfuscator[P]]

  /** Create a new DeObfuscator which de-obfuscate value using a custom formula.
    *
    * @param f
    *   the function which de-obfuscate the value
    */
  def of[P](f: KeyValueBuffer => P): DeObfuscator[P] = f(_)

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
    of { bufferTuple =>
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
    }

case class ObfuscationStrategy[P](obfuscator: Obfuscator[P], deObfuscator: DeObfuscator[P]):
  def bimap[U](fO: U => P, fD: P => U): ObfuscationStrategy[U] =
    ObfuscationStrategy[U](
      obfuscator   = Obfuscator.of(plain => obfuscator(fO(plain))),
      deObfuscator = DeObfuscator.of(bufferTuple => fD(deObfuscator(bufferTuple)))
    )

object ObfuscationStrategy extends ObfuscatorInstances:

  /** https://westonal.medium.com/protecting-strings-in-jvm-memory-84c365f8f01c
    *
    * We require a buffer that’s outside of the GCs control. This will ensure that multiple copies cannot be left beyond the time we are done with it.
    *
    * For this we can use ByteBuffer.allocateDirect The documentation for https://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html only says
    * a direct buffer may exist outside of the managed heap but it is at least pinned memory, as they are safe for I/O with non JVM code so the GC
    * won’t be moving this buffer and making copies.
    */
  def withDefaultDirectByteBuffer[P](capacity: Int)(
    fillBuffer: ByteBuffer => P => PlainValueBuffer,
    readBuffer: PlainValueBuffer => P
  ): ObfuscationStrategy[P] =
    ObfuscationStrategy(
      obfuscator   = Obfuscator.default((plainValue: P) => fillBuffer(ByteBuffer.allocateDirect(capacity)).apply(plainValue)),
      deObfuscator = DeObfuscator.default((buffer: PlainValueBuffer) => readBuffer(buffer.rewind().asReadOnlyBuffer()))
    )

  def defaultStringObfuscatorTuple(charset: Charset): ObfuscationStrategy[String] =
    summon[ObfuscationStrategy[Array[Byte]]].bimap(_.getBytes(charset), new String(_, charset))

private[secret] trait ObfuscatorInstances:

  given ObfuscationStrategy[Array[Byte]] =
    ObfuscationStrategy[Array[Byte]](
      obfuscator = Obfuscator.default((plainBytes: Array[Byte]) => ByteBuffer.allocateDirect(plainBytes.length).put(plainBytes)),
      deObfuscator = DeObfuscator.default((plainBuffer: PlainValueBuffer) => {
        val result = new Array[Byte](plainBuffer.capacity())
        plainBuffer.rewind().get(result)
        result
      })
    )

  given ObfuscationStrategy[String] =
    ObfuscationStrategy.defaultStringObfuscatorTuple(Charset.defaultCharset())

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

  given [P: ObfuscationStrategy]: Obfuscator[P] =
    summon[ObfuscationStrategy[P]].obfuscator

  given [P: ObfuscationStrategy]: DeObfuscator[P] =
    summon[ObfuscationStrategy[P]].deObfuscator
