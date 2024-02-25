package com.geirolz.secret.util

import at.favre.lib.crypto.bcrypt.BCrypt
import com.geirolz.secret.util.BytesUtils.clearByteBuffer

import java.nio.ByteBuffer

trait Hasher:
  def hash(chars: => Array[Byte], maxSize: Int = -1): ByteBuffer

object Hasher:

  /** Hashes the given characters using BCrypt
    * @param chars
    *   the characters to hash, maximum 72 characters
    * @param maxSize
    *   the maximum size of the resulting hash
    * @return
    *   the hash of the given characters
    */
  given bcrypt: Hasher with
    def hash(bytes: => Array[Byte], maxSize: Int = -1): ByteBuffer =
      val cappedBytes: Array[Byte] = bytes.slice(0, 72)
      var result: Array[Byte]      = BCrypt.withDefaults().hash(6, cappedBytes)
      val len: Int                 = result.length
      val cappedLen = maxSize match
        case -1           => len
        case x if x > len => len
        case x            => x

      val bb = ByteBuffer.allocateDirect(cappedLen).put(result, 0, cappedLen)
      result = BytesUtils.clearByteArray(result)
      bb
