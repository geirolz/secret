package com.geirolz.secret.strategy.algebra

import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.strategy.SecretStrategyAlgebra
import com.geirolz.secret.{KeyValueBuffer, PlainValueBuffer}

import java.nio.ByteBuffer

private[strategy] object PlainSecretStrategyAlgebra:

  lazy val instance: SecretStrategyAlgebra = new SecretStrategyAlgebra:
    final def obfuscator[P](f: P => PlainValueBuffer): Obfuscator[P] =
      Obfuscator.of { (plain: P) => KeyValueBuffer(ByteBuffer.allocateDirect(0), f(plain)) }

    final def deObfuscator[P](f: PlainValueBuffer => P): DeObfuscator[P] =
      DeObfuscator.of { bufferTuple => f(bufferTuple.roObfuscatedBuffer) }
