package com.geirolz.secret.strategy.algebra

import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.strategy.SecretStrategyAlgebra
import com.geirolz.secret.util.BytesUtils.{clearByteArray, clearByteBuffer}
import com.geirolz.secret.util.KeyValueBuffer
import com.geirolz.secret.{KeyBuffer, ObfuscatedValueBuffer, PlainValueBuffer}

import java.nio.ByteBuffer
import java.security.SecureRandom

private[strategy] object XorSecretStrategyAlgebra:

  lazy val instance: SecretStrategyAlgebra = new SecretStrategyAlgebra:
    /** Create a new Obfuscator which obfuscate value using a Xor formula.
      *
      * Formula: `plainValue[i] ^ (key[len - i] ^ (len * i))`
      *
      * Example:
      * {{{
      *   //Index      =    1     2     3     4    5
      *   //Plain      = [0x01][0x02][0x03][0x04][0x05]
      *   //Key        = [0x9d][0x10][0xad][0x87][0x2b]
      *   //Obfuscated = [0x9c][0x12][0xae][0x83][0x2e]
      * }}}
      */
    final def obfuscator[P](f: P => PlainValueBuffer): Obfuscator[P] =

      def genKeyBuffer(secureRandom: SecureRandom, size: Int): KeyBuffer =
        val keyBuffer: ByteBuffer        = ByteBuffer.allocateDirect(size)
        var keyArray: Array[Byte] | Null = new Array[Byte](size)
        secureRandom.nextBytes(keyArray)
        keyBuffer.put(keyArray)
        keyArray = clearByteArray(keyArray)
        keyBuffer

      Obfuscator.of { (plain: P) =>
        val secureRandom: SecureRandom           = new SecureRandom()
        var plainBuffer: PlainValueBuffer | Null = f(plain)
        val capacity: Int                        = plainBuffer.capacity()
        val keyBuffer: KeyBuffer                 = genKeyBuffer(secureRandom, capacity)
        val valueBuffer: ObfuscatedValueBuffer   = ByteBuffer.allocateDirect(capacity)
        for (i <- 0 until capacity)
          valueBuffer.put(
            (
              //format: off
              plainBuffer.get(i) ^ (keyBuffer.get(capacity - 1 - i) ^ (capacity * i).toByte)
              //format: on
            ).toByte
          )

        // clear plainBuffer
        plainBuffer = clearByteBuffer(plainBuffer)

        new KeyValueBuffer(keyBuffer, valueBuffer)
      }

    /** Create a new DeObfuscator which de-obfuscate value using a Xor formula.
      *
      * Formula: `obfuscated[i] ^ (key[len - i] ^ (len * i))`
      *
      * Example:
      * {{{
      *   //Index      =    1     2     3     4    5
      *   //Obfuscated = [0x9c][0x12][0xae][0x83][0x2e]
      *   //Key        = [0x9d][0x10][0xad][0x87][0x2b]
      *   //Plain      = [0x01][0x02][0x03][0x04][0x05]
      * }}}
      */
    final def deObfuscator[P](f: PlainValueBuffer => P): DeObfuscator[P] =
      DeObfuscator.of { bufferTuple =>
        val capacity: Int                             = bufferTuple.roKeyBuffer.capacity()
        var plainValueBuffer: PlainValueBuffer | Null = ByteBuffer.allocateDirect(capacity)

        for (i <- 0 until capacity)
          plainValueBuffer.put(
            (
              //format: off
              bufferTuple.roObfuscatedBuffer.get(i) ^ (bufferTuple.roKeyBuffer.get(capacity - 1 - i) ^ (capacity * i).toByte)
              //format: on
            ).toByte
          )

        val result = f(plainValueBuffer.asReadOnlyBuffer())

        // clear plainValueBuffer
        plainValueBuffer = clearByteBuffer(plainValueBuffer)

        result
      }
