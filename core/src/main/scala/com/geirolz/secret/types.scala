package com.geirolz.secret

import cats.MonadError

import java.nio.ByteBuffer
import scala.util.control.NoStackTrace

type PlainValueBuffer               = ByteBuffer
type ObfuscatedValueBuffer          = ByteBuffer
type KeyBuffer                      = ByteBuffer
private type MonadSecretError[F[_]] = MonadError[F, ? >: SecretNoLongerValid]

case class SecretNoLongerValid() extends RuntimeException("This secret value is no longer valid") with NoStackTrace
