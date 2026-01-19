package com.geirolz.secret.utils

import com.geirolz.secret.util.BytesUtils
import weaver.SimpleIOSuite

import java.nio.ByteBuffer

class BytesUtilsSuite extends SimpleIOSuite:

  pureTest("clearByteArray") {
    val bytes: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
    BytesUtils.clearByteArray(bytes)
    expect(bytes.toList == List[Byte](0, 0, 0, 0, 0))
  }

  pureTest("clearByteBuffer - HeapByteBuffer") {
    val buffer = ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5))
    BytesUtils.clearByteBuffer(buffer)
    expect(buffer.array().toList == List[Byte](0, 0, 0, 0, 0))
  }

  pureTest("clearByteBuffer - DirectByteBuffer") {
    val buffer = ByteBuffer.allocateDirect(5)
    buffer.put(Array[Byte](1, 2, 3, 4, 5))
    BytesUtils.clearByteBuffer(buffer)

    val array = new Array[Byte](buffer.capacity())
    buffer.rewind().get(array)

    expect(array.toList == List[Byte](0, 0, 0, 0, 0))
  }
