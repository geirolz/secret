package com.geirolz.secret.encrypt

import cats.syntax.all.*
import cats.{Functor, MonadThrow}

trait Encryptor[O]:
  type OutTpe = O
  def encrypt[F[_]: MonadThrow](t: String): F[O]
  final def map[U](f: O => U): Encryptor[U] = new Encryptor[U]:
    override def encrypt[F[_]: MonadThrow](t: String): F[U] =
      Encryptor.this.encrypt(t).map(f)

object Encryptor extends EncryptorInstances:

  given Functor[Encryptor] = new Functor[Encryptor]:
    def map[A, B](fa: Encryptor[A])(f: A => B): Encryptor[B] = fa.map(f)

private transparent sealed trait EncryptorInstances
