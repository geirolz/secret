package com.geirolz.secret.utils

import com.geirolz.secret.internal.BytesUtils
import java.nio.ByteBuffer

class BytesUtilsSuite extends munit.FunSuite:

  test("clearByteArray") {
    val bytes: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
    BytesUtils.clearByteArray(bytes)
    assertEquals(bytes.toList, List[Byte](0, 0, 0, 0, 0))
  }

  test("clearByteBuffer - HeapByteBuffer") {
    val buffer = ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5))
    BytesUtils.clearByteBuffer(buffer)
    assertEquals(buffer.array().toList, List[Byte](0, 0, 0, 0, 0))
  }

  test("clearByteBuffer - DirectByteBuffer") {
    val buffer = ByteBuffer.allocateDirect(5)
    buffer.put(Array[Byte](1, 2, 3, 4, 5))
    BytesUtils.clearByteBuffer(buffer)

    val array = new Array[Byte](buffer.capacity())
    buffer.rewind().get(array)

    assertEquals(array.toList, List[Byte](0, 0, 0, 0, 0))
  }
