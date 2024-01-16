package com.geirolz.secret

import cats.MonadError

import java.nio.ByteBuffer

type PlainValueBuffer               = ByteBuffer
type ObfuscatedValueBuffer          = ByteBuffer
type KeyBuffer                      = ByteBuffer
private type MonadSecretError[F[_]] = MonadError[F, ? >: SecretNoLongerValid]
