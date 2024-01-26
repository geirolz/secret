package com.geirolz.secret

import cats.MonadError
import com.geirolz.secret.internal.Location

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer               = ByteBuffer
type ObfuscatedValueBuffer          = ByteBuffer
type KeyBuffer                      = ByteBuffer
private type MonadSecretError[F[_]] = MonadError[F, ? >: SecretNoLongerValid]

case class SecretNoLongerValid(location: Location)
    extends RuntimeException(s"This secret value is no longer valid.\nAlready used at: $location")
    with NoStackTrace
