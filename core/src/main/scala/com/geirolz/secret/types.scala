package com.geirolz.secret

import cats.{MonadError, Show}
import com.geirolz.secret.util.Location

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer               = ByteBuffer
type ObfuscatedValueBuffer          = ByteBuffer
type KeyBuffer                      = ByteBuffer
private type MonadSecretError[F[_]] = MonadError[F, ? >: SecretDestroyed]

case class SecretDestroyed(destroyedAt: Location)
    extends RuntimeException(s"This secret has been already destroyed.\nLocation: $destroyedAt")
    with NoStackTrace

object SecretDestroyed:
  given Show[SecretDestroyed] = Show(_.getMessage)
