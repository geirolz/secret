package com.geirolz.secret

import cats.MonadError
import com.geirolz.secret.internal.Location

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer               = ByteBuffer
type ObfuscatedValueBuffer          = ByteBuffer
type KeyBuffer                      = ByteBuffer
private type MonadSecretError[F[_]] = MonadError[F, ? >: SecretDestroyed]

case class SecretDestroyed(destroyedAt: Location)
    extends RuntimeException(s"This secret destroyed.\nAlready used at: $destroyedAt")
    with NoStackTrace
