package com.geirolz.secret

import cats.Eq
import cats.effect.IO
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyFactory}
import com.geirolz.secret.transform.Hasher
import com.geirolz.secret.util.SysEnv
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

import org.scalacheck.Prop.forAll
import weaver.*
import weaver.scalacheck.{*, given}

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.util.Try
import cats.Show

/** This is the test suite for Secret functionality using various SecretStrategy factories.
  *
  * For testing SecretApi check SecretApiSuite.
  *
  * This test suite tests the following:
  *   - Secret.fromEnv
  *   - Secret.deferred.fromEnv
  *   - Secret without recDestructionLocation
  *   - Option Secret getOrEmptySecret
  *   - Either Secret getOrEmptySecret
  */
object XorSecretSuite extends SecretSuite(using SecretStrategy.xorFactory)
object PlainSecretSuite extends SecretSuite(using SecretStrategy.plainFactory)

abstract class SecretSuite(using SecretStrategyFactory) extends SimpleIOSuite with Checkers:

  test("Secret.fromEnv") {
    given SysEnv[IO] = SysEnv.fromMap[IO](Map("TEST" -> "VALUE"))
    Secret
      .fromEnv[IO]("TEST")
      .flatMap(_.useAndDestroy(value => expect(value == "VALUE")))
  }

  test("Secret.deferred.fromEnv") {
    given SysEnv[IO] = SysEnv.fromMap[IO](Map("TEST" -> "VALUE"))
    Secret.deferred
      .fromEnv[IO]("TEST")
      .use(value => expect(value == "VALUE"))
  }

  pureTest("Secret without recDestructionLocation") {

    val secret = Secret("TEST", recDestructionLocation = false)
    secret.destroy()

    expect(secret.destructionLocation.isEmpty)
  }

  test("Option Secret getOrEmptySecret") {

    val someSecret: Option[Secret[String]] = Option(Secret("TEST"))
    val noneSecret: Option[Secret[String]] = None

    for {
      someCheck <- someSecret.getOrEmptySecret.useAndDestroy(value => expect(value == "TEST"))
      noneCheck <- noneSecret.getOrEmptySecret.useAndDestroy(value => expect(value == ""))
    } yield someCheck && noneCheck
  }

  test("Either Secret getOrEmptySecret") {

    val rightSecret: Either[String, Secret[String]] = Right(Secret("TEST"))
    val leftSecret: Either[String, Secret[String]]  = Left("ERROR")

    for {
      rightCheck <- rightSecret.getOrEmptySecret.useAndDestroy(value => expect(value == "TEST"))
      leftCheck  <- leftSecret.getOrEmptySecret.useAndDestroy(value => expect(value == ""))
    } yield rightCheck && leftCheck
  }
