package com.geirolz.secret.util

import at.favre.lib.crypto.bcrypt.BCrypt
import com.geirolz.secret.util.BytesUtils.clearByteBuffer

import java.nio.ByteBuffer

/** A trait that represents a hasher. A hasher is an object that can hash a sequence of characters into a hash.
  *
  * The hash is represented as a ByteBuffer.
  */
trait Hasher:
  def hash(chars: => Array[Byte], maxSize: Option[Int] = None): ByteBuffer

object Hasher:

  given bcrypt: Hasher with
    /** Hashes the given characters using BCrypt
      * @param bytes
      *   the characters to hash, maximum 72 characters
      * @param maxSize
      *   the maximum size of the resulting hash
      * @return
      *   the hash of the given characters
      */
    def hash(bytes: => Array[Byte], maxSize: Option[Int] = None): ByteBuffer =
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
