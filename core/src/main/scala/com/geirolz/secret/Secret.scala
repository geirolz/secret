package com.geirolz.secret

import cats.{Eq, MonadError, Show}
import com.geirolz.secret.Secret.{DeObfuscator, MonadSecretError, Obfuscator, ObfuscatorTuple, PlainValueBuffer, SecretNoLongerValid}
import com.geirolz.secret.utils.BytesUtils.{clearByteArray, clearByteBuffer}

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.SecureRandom
import scala.util.Try
import scala.util.control.NoStackTrace
import scala.util.hashing.{Hashing, MurmurHash3}

/** Memory-safe and type-safe secret value of type `T`.
  *
  * `Secret` does the best to avoid leaking information in memory and in the code BUT an attack is possible and I don't give any certainties or
  * guarantees about security using this class, you use it at your own risk. Code is open source, you can check the implementation and take your
  * decision consciously. I'll do my best to improve the security and documentation of this class.
  *
  * <b>Obfuscation</b>
  *
  * The value is obfuscated when creating the `Secret` instance using an implicit `Obfuscator` which, by default, transform the value into a xor-ed
  * `ByteBuffer` witch store bytes outside the JVM using direct memory access.
  *
  * The obfuscated value is de-obfuscated using an implicit `DeObfuscator` instance every time the method `use` is invoked which returns the original
  * value converting bytes back to `T` re-apply the xor.
  *
  * <b>API and Type safety</b>
  *
  * While obfuscating the value prevents or at least makes it harder to read the value from memory, Secret class API are designed to avoid leaking
  * information in other ways. Preventing developers to improperly use the secret value ( logging, etc...).
  *
  * Example
  * {{{
  *   val secretString: Secret[String]  = Secret("my_password")
  *   val database: F[Database]         = secretString.use(password => initDb(password))
  * }}}
  *
  * ** Credits **
  *   - Personal experience in companies where I worked
  *   - https://westonal.medium.com/protecting-strings-in-jvm-memory-84c365f8f01c
  *   - VisualVM
  *   - ChatGPT
  */
sealed trait Secret[T] extends AutoCloseable {

  import cats.syntax.all.*

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def evalUse[F[_]: MonadSecretError, U](f: T => F[U])(implicit deObfuscator: DeObfuscator[T]): F[U]

  /** Destroy the secret value by filling the obfuscated value with '\0'.
    *
    * This method is idempotent.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def destroy(): Unit

  /** Check if the secret is destroyed
    *
    * @return
    *   `true` if the secret is destroyed, `false` otherwise
    */
  def isDestroyed: Boolean

  /** Calculate the non-deterministic hash code for this Secret.
    *
    * This hash code is NOT the hash code of the original value. It is the hash code of the obfuscated value.
    *
    * Since the obfuscated value is based on a random key, the hash code will be different every time. This function is not deterministic.
    *
    * @return
    *   the hash code of this secret. If the secret is destroyed it will return `-1`.
    */
  def hashCode(): Int

  // ------------------------------------------------------------------

  /** Avoid this method if possible. Unsafely apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Throws `SecretNoLongerValid` if the secret is destroyed
    */
  final def unsafeUse[U](f: T => U)(implicit deObfuscator: DeObfuscator[T]): U =
    use[Either[SecretNoLongerValid, *], U](f).fold(throw _, identity)

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  final def use[F[_]: MonadSecretError, U](f: T => U)(implicit deObfuscator: DeObfuscator[T]): F[U] =
    evalUse[F, U](f.andThen(_.pure[F]))

  /** Alias for `use` with `Either[Throwable, *]` */
  final def useE[U](f: T => U)(implicit deObfuscator: DeObfuscator[T]): Either[SecretNoLongerValid, U] =
    use[Either[SecretNoLongerValid, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  final def useAndDestroy[F[_]: MonadSecretError, U](f: T => U)(implicit deObfuscator: DeObfuscator[T]): F[U] =
    evalUseAndDestroy[F, U](f.andThen(_.pure[F]))

  /** Alias for `useAndDestroy` with `Either[Throwable, *]` */
  final def useAndDestroyE[U](f: T => U)(implicit deObfuscator: DeObfuscator[T]): Either[SecretNoLongerValid, U] =
    useAndDestroy[Either[SecretNoLongerValid, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  final def evalUseAndDestroy[F[_]: MonadSecretError, U](f: T => F[U])(implicit deObfuscator: DeObfuscator[T]): F[U] =
    evalUse(f).map { u => destroy(); u }

  /** Alias for `destroy` */
  final override def close(): Unit = destroy()

  /** Safely compare this secret with the provided `Secret`.
    *
    * @return
    *   `true` if the secrets are equal, `false` if they are not equal or if one of the secret is destroyed
    */
  final def isEquals(that: Secret[T])(implicit deObfuscator: DeObfuscator[T]): Boolean =
    evalUse[Try, Boolean](value => that.use[Try, Boolean](_ == value)).getOrElse(false)

  /** Always returns `false`, use `isEqual` instead */
  final override def equals(obj: Any): Boolean = false

  /** @return
    *   always returns a static place holder string "** SECRET **" to avoid leaking information
    */
  final override def toString: String = Secret.placeHolder
}

object Secret extends Instances {

  val placeHolder: String = "** SECRET **"
  private[secret] type PlainValueBuffer      = ByteBuffer
  private[secret] type ObfuscatedValueBuffer = ByteBuffer
  private[secret] type KeyBuffer             = ByteBuffer
  private type MonadSecretError[F[_]]        = MonadError[F, ? >: SecretNoLongerValid]

  case class SecretNoLongerValid() extends RuntimeException("This secret value is no longer valid") with NoStackTrace
  private[Secret] class KeyValueTuple(
    _keyBuffer: KeyBuffer,
    _obfuscatedBuffer: ObfuscatedValueBuffer
  ) {

    val roKeyBuffer: KeyBuffer = _keyBuffer.asReadOnlyBuffer()

    val roObfuscatedBuffer: ObfuscatedValueBuffer = _obfuscatedBuffer.asReadOnlyBuffer()

    lazy val obfuscatedHashCode: Int = {
      val capacity           = roObfuscatedBuffer.capacity()
      var bytes: Array[Byte] = new scala.Array[Byte](capacity)
      for (i <- 0 until capacity) {
        bytes(i) = roObfuscatedBuffer.get(i)
      }
      val hashCode: Int = MurmurHash3.bytesHash(bytes)
      bytes = clearByteArray(bytes)

      hashCode
    }

    def destroy(): Unit = {
      clearByteBuffer(_keyBuffer)
      clearByteBuffer(_obfuscatedBuffer)
      ()
    }
  }

  def apply[T: Obfuscator](value: T): Secret[T] = {

    var bufferTuple: KeyValueTuple = Obfuscator[T].apply(value)

    new Secret[T] {

      override def evalUse[F[_]: MonadSecretError, U](f: T => F[U])(implicit deObfuscator: DeObfuscator[T]): F[U] =
        if (isDestroyed)
          implicitly[MonadSecretError[F]].raiseError(SecretNoLongerValid())
        else
          f(deObfuscator(bufferTuple))

      override def destroy(): Unit = {
        bufferTuple.destroy()
        bufferTuple = null
      }

      override def isDestroyed: Boolean =
        bufferTuple == null

      override def hashCode(): Int =
        if (isDestroyed) -1 else bufferTuple.obfuscatedHashCode
    }
  }

  // ---------------- OBFUSCATOR ----------------
  trait Obfuscator[P] extends (P => KeyValueTuple)
  object Obfuscator {

    def apply[P: Obfuscator]: Obfuscator[P] =
      implicitly[Obfuscator[P]]

    /** Create a new Obfuscator which obfuscate value using a custom formula.
      *
      * @param f
      *   the function which obfuscate the value
      */
    def of[P](f: P => KeyValueTuple): Obfuscator[P] = f(_)

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

      def genKeyBuffer(secureRandom: SecureRandom, size: Int): KeyBuffer = {
        val keyBuffer = ByteBuffer.allocateDirect(size)
        var keyArray  = new Array[Byte](size)
        secureRandom.nextBytes(keyArray)
        keyBuffer.put(keyArray)

        // clear keyArray
        keyArray = clearByteArray(keyArray)

        keyBuffer
      }

      of { (plain: P) =>
        val secureRandom: SecureRandom         = new SecureRandom()
        var plainBuffer: PlainValueBuffer      = f(plain)
        val capacity: Int                      = plainBuffer.capacity()
        val keyBuffer: KeyBuffer               = genKeyBuffer(secureRandom, capacity)
        val valueBuffer: ObfuscatedValueBuffer = ByteBuffer.allocateDirect(capacity)
        for (i <- 0 until capacity) {
          valueBuffer.put(
            (
              plainBuffer.get(i) ^ (keyBuffer.get(capacity - 1 - i) ^ (capacity * i).toByte)
            ).toByte
          )
        }

        // clear plainBuffer
        plainBuffer = clearByteBuffer(plainBuffer)

        new KeyValueTuple(keyBuffer, valueBuffer)
      }
    }
  }

  trait DeObfuscator[P] extends (KeyValueTuple => P)
  object DeObfuscator {

    def apply[P: DeObfuscator]: DeObfuscator[P] =
      implicitly[DeObfuscator[P]]

    /** Create a new DeObfuscator which de-obfuscate value using a custom formula.
      *
      * @param f
      *   the function which de-obfuscate the value
      */
    def of[P](f: KeyValueTuple => P): DeObfuscator[P] = f(_)

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

        for (i <- 0 until capacity) {
          plainValueBuffer.put(
            (
              bufferTuple.roObfuscatedBuffer.get(i) ^ (bufferTuple.roKeyBuffer.get(capacity - 1 - i) ^ (capacity * i).toByte)
            ).toByte
          )
        }

        val result = f(plainValueBuffer.asReadOnlyBuffer())

        // clear plainValueBuffer
        plainValueBuffer = clearByteBuffer(plainValueBuffer)

        result
      }
  }

  case class ObfuscatorTuple[P](obfuscator: Obfuscator[P], deObfuscator: DeObfuscator[P]) {
    def bimap[U](fO: U => P, fD: P => U): ObfuscatorTuple[U] =
      ObfuscatorTuple[U](
        obfuscator   = Obfuscator.of(plain => obfuscator(fO(plain))),
        deObfuscator = DeObfuscator.of(bufferTuple => fD(deObfuscator(bufferTuple)))
      )
  }
  object ObfuscatorTuple {

    /** https://westonal.medium.com/protecting-strings-in-jvm-memory-84c365f8f01c
      *
      * We require a buffer that’s outside of the GCs control. This will ensure that multiple copies cannot be left beyond the time we are done with
      * it.
      *
      * For this we can use ByteBuffer.allocateDirect The documentation for https://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html only
      * says a direct buffer may exist outside of the managed heap but it is at least pinned memory, as they are safe for I/O with non JVM code so the
      * GC won’t be moving this buffer and making copies.
      */
    def withDefaultDirectByteBuffer[P](capacity: Int)(
      fillBuffer: ByteBuffer => P => ByteBuffer,
      readBuffer: ByteBuffer => P
    ): ObfuscatorTuple[P] =
      ObfuscatorTuple(
        obfuscator   = Obfuscator.default((plainValue: P) => fillBuffer(ByteBuffer.allocateDirect(capacity)).apply(plainValue)),
        deObfuscator = DeObfuscator.default((buffer: PlainValueBuffer) => readBuffer(buffer.rewind().asReadOnlyBuffer()))
      )

    def defaultStringObfuscatorTuple(charset: Charset): ObfuscatorTuple[String] =
      defaultBytesArrayObfuscatorTuple.bimap(_.getBytes(charset), new String(_, charset))
  }
}
sealed trait Instances {

  implicit val defaultBytesArrayObfuscatorTuple: ObfuscatorTuple[Array[Byte]] =
    ObfuscatorTuple[Array[Byte]](
      obfuscator = Obfuscator.default((plainBytes: Array[Byte]) => ByteBuffer.allocateDirect(plainBytes.length).put(plainBytes)),
      deObfuscator = DeObfuscator.default((plainBuffer: PlainValueBuffer) => {
        val result = new Array[Byte](plainBuffer.capacity())
        plainBuffer.rewind().get(result)
        result
      })
    )

  implicit val defaultStdCharsetStringObfuscatorTuple: ObfuscatorTuple[String] =
    ObfuscatorTuple.defaultStringObfuscatorTuple(Charset.defaultCharset())

  implicit val xorByteObfuscatorTuple: ObfuscatorTuple[Byte] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(1)(_.put, _.get)

  implicit val defaultCharObfuscatorTuple: ObfuscatorTuple[Char] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(2)(_.putChar, _.getChar)

  implicit val defaultShortObfuscatorTuple: ObfuscatorTuple[Short] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(2)(_.putShort, _.getShort)

  implicit val defaultIntObfuscatorTuple: ObfuscatorTuple[Int] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(4)(_.putInt, _.getInt)

  implicit val defaultLongObfuscatorTuple: ObfuscatorTuple[Long] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(8)(_.putLong, _.getLong)

  implicit val defaultFloatObfuscatorTuple: ObfuscatorTuple[Float] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(4)(_.putFloat, _.getFloat)

  implicit val defaultDoubleObfuscatorTuple: ObfuscatorTuple[Double] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(8)(_.putDouble, _.getDouble)

  implicit val defaultBoolObfuscatorTuple: ObfuscatorTuple[Boolean] =
    ObfuscatorTuple.withDefaultDirectByteBuffer(1)(
      (b: PlainValueBuffer) => (v: Boolean) => b.put(if (v) 1.toByte else 0.toByte),
      _.get == 1.toByte
    )

  implicit val defaultBigIntObfuscatorTuple: ObfuscatorTuple[BigInt] =
    defaultBytesArrayObfuscatorTuple.bimap(_.toByteArray, BigInt(_))

  implicit val defaultBigDecimalObfuscatorTuple: ObfuscatorTuple[BigDecimal] =
    defaultStdCharsetStringObfuscatorTuple.bimap(_.toString, str => BigDecimal(str))

  implicit def unzipObfuscatorTupleToObfuscator[P: ObfuscatorTuple]: Obfuscator[P] =
    implicitly[ObfuscatorTuple[P]].obfuscator

  implicit def unzipObfuscatorTupleToDeObfuscator[P: ObfuscatorTuple]: DeObfuscator[P] =
    implicitly[ObfuscatorTuple[P]].deObfuscator

  implicit def hashing[T]: Hashing[Secret[T]] =
    Hashing.fromFunction(_.hashCode())

  implicit def eq[T]: Eq[Secret[T]] =
    Eq.fromUniversalEquals

  implicit def show[T]: Show[Secret[T]] =
    Show.fromToString
}
