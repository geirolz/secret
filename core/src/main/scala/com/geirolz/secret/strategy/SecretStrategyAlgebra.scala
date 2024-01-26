package com.geirolz.secret.strategy

import com.geirolz.secret.PlainValueBuffer
import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.strategy.algebra.{PlainSecretStrategyAlgebra, XorSecretStrategyAlgebra}

trait SecretStrategyAlgebra:
  def obfuscator[P](f: P => PlainValueBuffer): Obfuscator[P]
  def deObfuscator[P](f: PlainValueBuffer => P): DeObfuscator[P]
  def newFactory: SecretStrategyFactory = SecretStrategyFactory(this)

object SecretStrategyAlgebra:
  lazy val plain: SecretStrategyAlgebra = PlainSecretStrategyAlgebra.instance
  lazy val xor: SecretStrategyAlgebra   = XorSecretStrategyAlgebra.instance
