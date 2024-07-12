package com.geirolz.secret

import cats.{MonadError, Show}
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.{Hasher, Location}

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer      = ByteBuffer
type ObfuscatedValueBuffer = ByteBuffer
type KeyBuffer             = ByteBuffer

// alias
type SString = Secret[String]
object SString:
  type OneShot = OneShotSecret[String]
  def oneShot(value: String, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[String],
    hasher: Hasher
  ): SString.OneShot = OneShotSecret(value)

  def apply(value: String, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[String],
    hasher: Hasher
  ): SString = Secret(value)

// private
private[secret] type MonadSecretError[F[_]] = MonadError[F, ? >: SecretDestroyed]
private[secret] val vaultTag: String     = "** VAULT **"
private[secret] val secretTag: String    = "** SECRET **"
private[secret] val destroyedTag: String = "** DESTROYED **"
