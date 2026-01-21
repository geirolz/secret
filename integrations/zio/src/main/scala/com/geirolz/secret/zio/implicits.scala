package com.geirolz.secret.zio

import cats.MonadError
import com.geirolz.secret.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import _root_.zio.interop.catz.*
import _root_.zio.{Scope, ZIO}

import scala.annotation.targetName

extension [T](secret: Secret.OneShot[T])
  inline def scopedDestroy: ZIO[Scope, SecretDestroyed, T] =
    ZIO
      .acquireRelease(
        ZIO.succeed(secret)
      )(s => ZIO.succeed(s.destroy()))
      .flatMap(_.accessValue[ZIO[Any, SecretDestroyed, *]])

extension (obj: OneShotSecret.type)
  def scoped[T: SecretStrategy](secret: T)(using Hasher): ZIO[Scope, SecretDestroyed, T] =
    OneShotSecret(secret).scopedDestroy

extension [T](secret: Secret[T])
  inline def scoped: ZIO[Scope, SecretDestroyed, T] =
    ZIO
      .acquireRelease(
        ZIO.succeed(secret.duplicate)
      )(s => ZIO.succeed(s.destroy()))
      .flatMap(_.accessValue[ZIO[Any, SecretDestroyed, *]])

  inline def scopedDestroy: ZIO[Scope, SecretDestroyed, T] =
    ZIO
      .acquireRelease(
        ZIO.succeed(secret)
      )(s => ZIO.succeed(s.destroy()))
      .flatMap(_.accessValue[ZIO[Any, SecretDestroyed, *]])

extension (obj: Secret.type)
  @targetName("SecretScoped")
  def scoped[T: SecretStrategy](secret: T)(using DummyImplicit, Hasher): ZIO[Scope, SecretDestroyed, T] =
    Secret(secret).scopedDestroy

extension [T](secret: Secret.Deferred[ZIO[Any, Throwable, *], T])
  inline def scoped: ZIO[Scope, Throwable, T] =
    ZIO
      .acquireRelease(
        secret.acquire
      )(s => ZIO.succeed(s.destroy()))
      .flatMap(s => ZIO.fromEither(s.euse(identity)))

extension (obj: DeferredSecret.type)
  def scoped[T: SecretStrategy](acquire: => ZIO[Any, Throwable, T])(using Hasher): ZIO[Scope, Throwable, T] =
    DeferredSecret(acquire).scoped
