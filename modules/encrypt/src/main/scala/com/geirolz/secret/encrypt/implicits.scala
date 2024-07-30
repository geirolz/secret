package com.geirolz.secret.encrypt

import cats.MonadThrow
import cats.syntax.all.*
import com.geirolz.secret.Secret
import com.geirolz.secret.internal.{SecretApi, SecretCompanionApi}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import com.geirolz.secret.util.Location

extension [S[X] <: SecretApi[X]: SecretCompanionApi, O: SecretStrategy](secret: S[String])

  def encryptAndDestroy[F[_]: MonadThrow](
    encryptor: Encryptor[O]
  )(using Location): F[S[O]] =
    secret
      .evalUseAndDestroy(v => encryptor.encrypt[F](v))
      .map(encrypted => summon[SecretCompanionApi[S]].apply(encrypted))

  def encryptAndDestroyDeferred[F[_]: MonadThrow](
    encryptor: Encryptor[O]
  )(using Location): Secret.Deferred[F, O] =
    Secret.deferred.fromSecret(
      secret
        .evalUseAndDestroy(v => encryptor.encrypt[F](v))
        .map(encrypted => Secret(encrypted))
    )

extension [O: SecretStrategy](secret: Secret[String])

  def encrypt[F[_]: MonadThrow](
    encryptor: Encryptor[O]
  )(using Location): F[Secret[O]] =
    secret
      .evalUse(v => encryptor.encrypt[F](v))
      .map(encrypted => Secret(encrypted))

  def encryptDeferred[F[_]: MonadThrow](
    encryptor: Encryptor[O]
  )(using Location): Secret.Deferred[F, O] =
    Secret.deferred.fromSecret(
      secret
        .evalUse(v => encryptor.encrypt[F](v))
        .map(encrypted => Secret(encrypted))
    )
