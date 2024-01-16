package com.geirolz.secret.typesafe.config

import com.typesafe.config.Config

trait TypesafeConfigSecretAdapter[P]:
  def get(config: Config, path: String): P

object TypesafeConfigSecretAdapter:

  import scala.jdk.CollectionConverters.*

  def apply[P: TypesafeConfigSecretAdapter]: TypesafeConfigSecretAdapter[P] =
    summon[TypesafeConfigSecretAdapter[P]]

  def of[P](f: Config => String => P): TypesafeConfigSecretAdapter[P] =
    (config: Config, path: String) => f(config)(path)

  // numbers
  given TypesafeConfigSecretAdapter[String]     = of(_.getString)
  given TypesafeConfigSecretAdapter[Int]        = of(_.getInt)
  given TypesafeConfigSecretAdapter[Short]      = of(c => p => c.getInt(p).toShort)
  given TypesafeConfigSecretAdapter[Long]       = of(_.getLong)
  given TypesafeConfigSecretAdapter[Float]      = of(c => p => c.getDouble(p).toFloat)
  given TypesafeConfigSecretAdapter[Double]     = of(_.getDouble)
  given TypesafeConfigSecretAdapter[BigInt]     = of(c => p => BigInt(c.getLong(p)))
  given TypesafeConfigSecretAdapter[BigDecimal] = of(c => p => BigDecimal(c.getDouble(p)))

  // other
  given TypesafeConfigSecretAdapter[Array[Byte]] = of(c => p => c.getBytesList(p).asScala.toArray.map(_.toByte))
  given TypesafeConfigSecretAdapter[Boolean]     = of(_.getBoolean)
