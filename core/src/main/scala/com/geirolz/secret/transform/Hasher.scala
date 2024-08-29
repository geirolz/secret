package com.geirolz.secret.transform

import at.favre.lib.crypto.bcrypt.BCrypt
import cats.MonadThrow
import cats.syntax.all.*
import com.geirolz.secret.util.BytesUtils

import java.nio.ByteBuffer
import java.security.MessageDigest

/** A trait that represents an hasher.
  *
  * An hasher is an object that can hash a sequence of bytes.
  *
  * The hash is represented as a ByteBuffer.
  */
trait Hasher:
  def hash(bytes: => Array[Byte]): ByteBuffer
  def hashAsString(bytes: => Array[Byte]): String

object Hasher:

  given default: Hasher = bcrypt(12.some)

  def bcrypt(maxSize: Option[Int] = None): Hasher =
    new Hasher:
      override def hash(bytes: => Array[Byte]): ByteBuffer =
        val cappedBytes: Array[Byte] = bytes.slice(0, 72)
        var result: Array[Byte]      = BCrypt.withDefaults().hash(6, cappedBytes)
        val len: Int                 = result.length
        val cappedLen = maxSize match
          case None               => len
          case Some(x) if x > len => len
          case Some(x)            => x

        val bb = ByteBuffer.allocateDirect(cappedLen).put(result, 0, cappedLen)
        result = BytesUtils.clearByteArray(result)
        bb

      override def hashAsString(bytes: => Array[Byte]): String =
        hash(bytes).array().mkString

  def fromMessageDigest[F[_]: MonadThrow](algorithm: String): F[Hasher] =
    MonadThrow[F].catchNonFatal {
      val md = MessageDigest.getInstance(algorithm)

      new Hasher:
        override def hash(bytes: => Array[Byte]): ByteBuffer =
          md.update(bytes)
          ByteBuffer.wrap(md.digest())

        override def hashAsString(bytes: => Array[Byte]): String =
          hash(bytes)
            .array()
            .map("%02x".format(_))
            .mkString
    }

  // ------------------------------- MD -------------------------------
  enum MD:
    case `MD2`, `MD5`

  def md[F[_]: MonadThrow](md: MD): F[Hasher] =
    fromMessageDigest(
      md match
        case MD.`MD2` => "MD2"
        case MD.`MD5` => "MD5"
    )

  inline def md2[F[_]: MonadThrow]: F[Hasher] = md(MD.`MD2`)
  inline def md5[F[_]: MonadThrow]: F[Hasher] = md(MD.`MD5`)

  // ------------------------------- SHA -------------------------------
  enum SHA:
    case `SHA-1`, `SHA-256`, `SHA-384`, `SHA-512`, `SHA3-256`, `SHA3-384`, `SHA3-512`

  def sha[F[_]: MonadThrow](sha: SHA): F[Hasher] =
    fromMessageDigest[F](
      sha match

        // SHA-1
        case SHA.`SHA-1` => "SHA-1"

        // SHA-2
        case SHA.`SHA-256` => "SHA-256"
        case SHA.`SHA-384` => "SHA-384"
        case SHA.`SHA-512` => "SHA-512"

        // SHA-3
        case SHA.`SHA3-256` => "SHA3-256"
        case SHA.`SHA3-384` => "SHA3-384"
        case SHA.`SHA3-512` => "SHA3-512"
    )

  // sha1
  inline def sha1[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA-1`)

  // sha2
  inline def sha256[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA-256`)
  inline def sha384[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA-384`)
  inline def sha512[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA-512`)

  // sha3
  inline def sha3_256[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA3-256`)
  inline def sha3_384[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA3-384`)
  inline def sha3_512[F[_]: MonadThrow]: F[Hasher] = sha(SHA.`SHA3-512`)
