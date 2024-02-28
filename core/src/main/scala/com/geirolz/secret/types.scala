package com.geirolz.secret

import cats.{MonadError, Show}
import com.geirolz.secret.util.Location

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer                       = ByteBuffer
type ObfuscatedValueBuffer                  = ByteBuffer
type KeyBuffer                              = ByteBuffer
private[secret] type MonadSecretError[F[_]] = MonadError[F, ? >: SecretDestroyed]
private[secret] val vaultTag: String     = "** VAULT **"
private[secret] val secretTag: String    = "** SECRET **"
private[secret] val destroyedTag: String = "** DESTROYED **"
