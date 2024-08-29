package com.geirolz.secret

import cats.MonadError

import java.nio.ByteBuffer

type PlainValueBuffer      = ByteBuffer
type ObfuscatedValueBuffer = ByteBuffer
type KeyBuffer             = ByteBuffer

// private
private[secret] type MonadSecretError[F[_]] = MonadError[F, ? >: SecretDestroyed]
private[secret] val vaultTag: String     = "** VAULT **"
private[secret] val secretTag: String    = "** SECRET **"
private[secret] val destroyedTag: String = "** DESTROYED **"
