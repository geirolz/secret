package com.geirolz.secret.internal

import java.nio.ByteBuffer
import java.util

private[geirolz] object BytesUtils:

  def clearByteArray(bytes: Array[Byte]): Null =
    util.Arrays.fill(bytes, 0.toByte)
    null

  def clearByteBuffer(buffer: ByteBuffer): Null =
    val zeroBytesArray = new Array[Byte](buffer.capacity())
    util.Arrays.fill(zeroBytesArray, 0.toByte)
    buffer.clear()
    buffer.put(zeroBytesArray)
    null
