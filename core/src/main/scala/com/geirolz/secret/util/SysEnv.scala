package com.geirolz.secret.util

import cats.syntax.all.*
import cats.{Applicative, MonadThrow}

/** A simple type class to get environment variables
  * @tparam F
  *   the effect type
  */
trait SysEnv[F[_]]:

  /** Get the value of an environment variable
    * @param key
    *   the name of the environment variable
    * @return
    *   the value of the environment variable, if it exists
    */
  def getEnv(key: String): F[Option[String]]

object SysEnv:
  inline def apply[F[_]](using ev: SysEnv[F]): SysEnv[F] = ev

  def fromMap[F[_]: Applicative](values: Map[String, String]): SysEnv[F] =
    (key: String) => values.get(key).pure[F]

  given [F[_]: MonadThrow]: SysEnv[F] = new SysEnv[F] {
    def getEnv(key: String): F[Option[String]] =
      MonadThrow[F].catchNonFatal(Option(System.getenv(key)))
  }
