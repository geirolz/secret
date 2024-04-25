package com.geirolz.secret

import cats.{MonadError, Show}
import com.geirolz.secret.util.Location

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer      = ByteBuffer
type ObfuscatedValueBuffer = ByteBuffer
type KeyBuffer             = ByteBuffer

// alias
type SPassword = Secret[String]
object SPassword:
  type OneShot = OneShotSecret[String]
  def apply(value: String): SPassword = Secret(value)

// private
private[secret] type MonadSecretError[F[_]] = MonadError[F, ? >: SecretDestroyed]
private[secret] val vaultTag: String     = "** VAULT **"
private[secret] val secretTag: String    = "** SECRET **"
private[secret] val destroyedTag: String = "** DESTROYED **"
