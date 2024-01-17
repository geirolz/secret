package com.geirolz.secret

import com.geirolz.secret.internal.BytesUtils.{clearByteArray, clearByteBuffer}
import com.geirolz.secret.{KeyBuffer, ObfuscatedValueBuffer}

import java.nio.ByteBuffer
import scala.util.hashing.MurmurHash3

class KeyValueBuffer(_keyBuffer: KeyBuffer, _obfuscatedBuffer: ObfuscatedValueBuffer) extends AutoCloseable:
  val roKeyBuffer: KeyBuffer                    = _keyBuffer.asReadOnlyBuffer()
  val roObfuscatedBuffer: ObfuscatedValueBuffer = _obfuscatedBuffer.asReadOnlyBuffer()

  lazy val obfuscatedHashCode: Int =
    val capacity           = roObfuscatedBuffer.capacity()
    var bytes: Array[Byte] = new scala.Array[Byte](capacity)
    for (i <- 0 until capacity)
      bytes(i) = roObfuscatedBuffer.get(i)

    val hashCode: Int = MurmurHash3.bytesHash(bytes)
    bytes = clearByteArray(bytes)
    hashCode

  def destroy(): Unit =
    clearByteBuffer(_keyBuffer)
    clearByteBuffer(_obfuscatedBuffer)
    ()

  def close(): Unit = destroy()

object KeyValueBuffer:
  def directEmpty(capacity: Int) = new KeyValueBuffer(
    _keyBuffer        = ByteBuffer.allocateDirect(capacity),
    _obfuscatedBuffer = ByteBuffer.allocateDirect(capacity)
  )
