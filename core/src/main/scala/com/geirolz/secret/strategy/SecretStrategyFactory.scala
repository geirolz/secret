package com.geirolz.secret.strategy

import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.{strategy, PlainValueBuffer}

import java.nio.ByteBuffer
import java.nio.charset.Charset
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/** Factory for [[SecretStrategy]].
  *
  * Implicitly provides [[SecretStrategy]] for primitive types and collections.
  *
  * @param algebra
  *   the algebra used to build the [[SecretStrategy]].
  */
open class SecretStrategyFactory private[secret] (algebra: SecretStrategyAlgebra):

  def apply[U](f: SecretStrategyFactory ?=> U): U =
    f(using this)

  def build[P](capacity: Int)(
    fillBuffer: ByteBuffer => P => PlainValueBuffer,
    readBuffer: PlainValueBuffer => P
  ): SecretStrategy[P] =
    withDirectByteBuffer(capacity)(
      buildObfuscator   = algebra.obfuscator,
      buildDeObfuscator = algebra.deObfuscator,
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
      buildObfuscator((plainValue: P) => fillBuffer(ByteBuffer.allocateDirect(capacity)).apply(plainValue)),
      buildDeObfuscator((buffer: PlainValueBuffer) => readBuffer(buffer.rewind().asReadOnlyBuffer()))
    )

  def directByteBufferForArray[U: ClassTag](
    toBytes: Array[U] => Array[Byte],
    toUs: Array[Byte] => Array[U]
  ): SecretStrategy[Array[U]] =
    strategy.SecretStrategy[Array[U]](
      obfuscator = algebra.obfuscator((plainBytes: Array[U]) =>
        ByteBuffer.allocateDirect(plainBytes.length).put(toBytes(plainBytes))
      ),
      deObfuscator = algebra.deObfuscator((plainBuffer: PlainValueBuffer) => {
        val result = new Array[Byte](plainBuffer.capacity())
        plainBuffer.rewind().get(result)
        toUs(result)
      })
    )

  def forString(charset: Charset)(using ss: SecretStrategy[Array[Byte]]): SecretStrategy[String] =
    ss.bimap(_.getBytes(charset), new String(_, charset))

  given SecretStrategy[Short] =
    build(2)(_.putShort, _.getShort)

  given SecretStrategy[Int] =
    build(4)(_.putInt, _.getInt)

  given SecretStrategy[Long] =
    build(8)(_.putLong, _.getLong)

  given SecretStrategy[Float] =
    build(4)(_.putFloat, _.getFloat)

  given SecretStrategy[Double] =
    build(8)(_.putDouble, _.getDouble)

  given SecretStrategy[BigInt] =
    secretStrategyForBytes.bimap(_.toByteArray, BigInt(_))

  given SecretStrategy[BigDecimal] =
    summon[SecretStrategy[String]].bimap(_.toString, str => BigDecimal(str))

  // other
  given SecretStrategy[String] =
    forString(Charset.defaultCharset())

  given SecretStrategy[Boolean] =
    build(1)(
      fillBuffer = (b: PlainValueBuffer) => (v: Boolean) => b.put(if v then 1.toByte else 0.toByte),
      readBuffer = _.get == 1.toByte
    )

  given SecretStrategy[Byte] =
    build(1)(_.put, _.get)

  given SecretStrategy[Char] =
    build(2)(_.putChar, _.getChar)

  // collections
  given secretStrategyForBytes: SecretStrategy[Array[Byte]] =
    directByteBufferForArray(identity, identity)

  given [T: ClassTag](using ss: SecretStrategy[String]): SecretStrategy[Array[Char]] =
    ss.bimap(new String(_), _.toCharArray)

  given [T: ClassTag](using ss: SecretStrategy[Array[T]]): SecretStrategy[ArraySeq[T]] =
    ss.bimap(_.toArray, ArraySeq.from)
