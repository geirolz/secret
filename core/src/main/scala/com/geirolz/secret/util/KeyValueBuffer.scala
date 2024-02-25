package com.geirolz.secret.util

import com.geirolz.secret.util.BytesUtils.clearByteBuffer
import com.geirolz.secret.{KeyBuffer, ObfuscatedValueBuffer}

import java.nio.ByteBuffer

class KeyValueBuffer(_keyBuffer: KeyBuffer, _obfuscatedBuffer: ObfuscatedValueBuffer) extends AutoCloseable:
  val roKeyBuffer: KeyBuffer                    = _keyBuffer.asReadOnlyBuffer()
  val roObfuscatedBuffer: ObfuscatedValueBuffer = _obfuscatedBuffer.asReadOnlyBuffer()

  def destroy(): Unit =
    clearByteBuffer(_keyBuffer)
    clearByteBuffer(_obfuscatedBuffer)
    ()

  inline def close(): Unit = destroy()

object KeyValueBuffer:
  def directEmpty(capacity: Int): KeyValueBuffer = new KeyValueBuffer(
    _keyBuffer        = ByteBuffer.allocateDirect(capacity),
    _obfuscatedBuffer = ByteBuffer.allocateDirect(capacity)
  )
