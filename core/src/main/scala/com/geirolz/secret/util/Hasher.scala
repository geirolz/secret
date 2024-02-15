package com.geirolz.secret.util

import at.favre.lib.crypto.bcrypt.BCrypt

import java.nio.ByteBuffer

trait Hasher:
  def hash(chars: => Array[Char]): ByteBuffer

object Hasher:
  given bcrypt: Hasher with
    def hash(chars: => Array[Char]): ByteBuffer =
      var result: Array[Byte] = BCrypt.withDefaults().hash(10, chars)
      val bb                  = ByteBuffer.allocateDirect(result.length).put(result)
      result = BytesUtils.clearByteArray(result)
      bb
