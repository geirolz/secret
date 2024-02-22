package com.geirolz.secret.util

import at.favre.lib.crypto.bcrypt.BCrypt

import java.nio.ByteBuffer

trait Hasher:
  def hash(chars: => Array[Char], maxSize: Int = -1): ByteBuffer

object Hasher:
  given bcrypt: Hasher with
    def hash(chars: => Array[Char], maxSize: Int = -1): ByteBuffer =
      var result: Array[Byte] = BCrypt.withDefaults().hash(10, chars)
      val len: Int            = result.length
      val cappedLen = maxSize match
        case -1           => len
        case x if x > len => len
        case x            => x

      val bb = ByteBuffer.allocateDirect(cappedLen).put(result.slice(0, cappedLen))
      result = BytesUtils.clearByteArray(result)
      bb
