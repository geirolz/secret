package com.geirolz.secret.strategy

import com.geirolz.secret.PlainValueBuffer
import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.strategy.algebra.{PlainSecretStrategyAlgebra, XorSecretStrategyAlgebra}

/** A SecretStrategyAlgebra defines the operators to build Obfuscator and DeObfuscator instances.
  *
  * The algebra also provides a factory to create SecretStrategy instances.
  */
trait SecretStrategyAlgebra:
  def obfuscator[P](f: P => PlainValueBuffer): Obfuscator[P]
  def deObfuscator[P](f: PlainValueBuffer => P): DeObfuscator[P]
  def newFactory: SecretStrategyFactory = SecretStrategyFactory(this)

object SecretStrategyAlgebra:
  lazy val plain: SecretStrategyAlgebra = PlainSecretStrategyAlgebra.instance
  lazy val xor: SecretStrategyAlgebra   = XorSecretStrategyAlgebra.instance
