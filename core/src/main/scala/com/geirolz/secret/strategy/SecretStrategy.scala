package com.geirolz.secret.strategy

import com.geirolz.secret.*
import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}

opaque type SecretStrategy[P] = (Obfuscator[P], DeObfuscator[P])
object SecretStrategy extends SecretStrategyFactory(SecretStrategyAlgebra.xor):

  final lazy val plainFactory = SecretStrategyAlgebra.plain.newFactory
  final lazy val xorFactory   = SecretStrategyAlgebra.xor.newFactory

  extension [P](strategy: SecretStrategy[P])
    def obfuscator: Obfuscator[P]     = strategy._1
    def deObfuscator: DeObfuscator[P] = strategy._2
    def bimap[U](fO: U => P, fD: P => U): SecretStrategy[U] =
      SecretStrategy(
        obfuscator   = Obfuscator(plain => obfuscator(fO(plain))),
        deObfuscator = DeObfuscator(bufferTuple => fD(deObfuscator(bufferTuple)))
      )

  def apply[P](using ss: SecretStrategy[P]): SecretStrategy[P] = ss

  inline def apply[P](obfuscator: Obfuscator[P], deObfuscator: DeObfuscator[P]): SecretStrategy[P] =
    (obfuscator, deObfuscator)

  // ------------------ Obfuscator ------------------
  opaque type Obfuscator[P] = P => KeyValueBuffer
  object Obfuscator:

    extension [P](obfuscator: Obfuscator[P])
      /** Obfuscate a plain value. */
      def apply(plain: P): KeyValueBuffer = obfuscator(plain)

    /** Create a new Obfuscator from a function. */
    def of[P](f: P => KeyValueBuffer): Obfuscator[P] = f

  // ------------------ DeObfuscator ------------------
  opaque type DeObfuscator[P] = KeyValueBuffer => P
  object DeObfuscator:

    extension [P](obfuscator: DeObfuscator[P])
      /** DeObfuscate an obfuscated value. */
      def apply(bufferTuple: KeyValueBuffer): P = obfuscator(bufferTuple)

    /** Create a new DeObfuscator from a function. */
    def of[P](f: KeyValueBuffer => P): DeObfuscator[P] = f
