package com.geirolz.secret.util

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}
import java.util

private[secret] object BytesUtils:

  def clearByteArray(bytes: Array[Byte]): Null =
    util.Arrays.fill(bytes, 0.toByte)
    null

  def clearByteBuffer(buffer: ByteBuffer): Null =
    val zeroBytesArray = new Array[Byte](buffer.capacity())
    util.Arrays.fill(zeroBytesArray, 0.toByte)
    buffer.clear()
    buffer.put(zeroBytesArray)
    null

  def asString(
    buffer: => ByteBuffer,
    start: Int       = 0,
    len: Int         = 0,
    charset: Charset = StandardCharsets.UTF_8
  ): String =
    require(buffer != null, "buffer cannot be null")
    require(start >= 0, "start cannot be negative")
    require(len >= 0, "len cannot be negative")
    require((start + len) <= buffer.limit(), "start + len cannot be greater than buffer limit")
    val lenF  = if (len == 0) buffer.limit() - start else len
    val bytes = new Array[Byte](lenF)
    for (i <- 0 until lenF) do bytes(i) = buffer.get(start + i)
    new String(bytes, charset)
