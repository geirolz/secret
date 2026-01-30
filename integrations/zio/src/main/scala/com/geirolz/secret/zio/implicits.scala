package com.geirolz.secret.zio

import cats.MonadError
import com.geirolz.secret.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import _root_.zio.interop.catz.*
import _root_.zio.{Scope, Task, ZIO}
import _root_.zio.managed.*

import scala.annotation.targetName

extension [T](secret: Secret.OneShot[T])
  inline def managedDestroy: ZManaged[Any, Throwable, T] =
    ZManaged
      .acquireReleaseWith(ZIO.succeed(secret))(s => ZIO.succeed(s.destroy()))
      .mapZIO(_.accessValue[Task])

extension (obj: OneShotSecret.type)
  def managed[T: SecretStrategy](secret: T)(using Hasher): ZManaged[Any, Throwable, T] =
    OneShotSecret(secret).managedDestroy

extension [T](secret: Secret[T])
  inline def managed: ZManaged[Any, Throwable, T] =
    ZManaged
      .acquireReleaseWith(ZIO.succeed(secret.duplicate))(s => ZIO.succeed(s.destroy()))
      .mapZIO(_.accessValue[Task])

  inline def managedDestroy: ZManaged[Any, Throwable, T] =
    ZManaged
      .acquireReleaseWith(ZIO.succeed(secret))(s => ZIO.succeed(s.destroy()))
      .mapZIO(_.accessValue[Task])

extension (obj: Secret.type)
  @targetName("SecretScoped")
  def managed[T: SecretStrategy](secret: T)(using DummyImplicit, Hasher): ZManaged[Any, Throwable, T] =
    Secret(secret).managedDestroy

extension [T](secret: Secret.Deferred[Task, T])
  inline def managed: ZManaged[Any, Throwable, T] =
    ZManaged
      .acquireReleaseWith(secret.acquire)(s => ZIO.succeed(s.destroy()))
      .mapZIO(_.accessValue[Task])

extension (obj: DeferredSecret.type)
  def managed[T: SecretStrategy](acquire: => Task[T])(using Hasher): ZManaged[Any, Throwable, T] =
    DeferredSecret(acquire).managed
