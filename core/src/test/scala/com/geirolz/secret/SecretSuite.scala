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

class XorSecretSuite extends SecretSuite(using SecretStrategy.xorFactory)
class PlainSecretSuite extends SecretSuite(using SecretStrategy.plainFactory)

abstract class SecretSuite(using SecretStrategyFactory) extends SimpleIOSuite with Checkers:

  // numbers
  testSecretStrategyFor[Short]
  testSecretStrategyFor[Int]
  testSecretStrategyFor[Long]
  testSecretStrategyFor[Float]
  testSecretStrategyFor[Double]
  testSecretStrategyFor[BigInt]
  testSecretStrategyFor[BigDecimal]

  // others
  testSecretStrategyFor[String]
  testSecretStrategyFor[Boolean]
  testSecretStrategyFor[Byte]
  testSecretStrategyFor[Char]

  // collections
  testSecretStrategyFor[ArraySeq[Byte]]
  testSecretStrategyFor[ArraySeq[Char]]

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

  private def testSecretStrategyFor[T: Arbitrary: Eq: SecretStrategy: Show](using
    c: ClassTag[T]
  ): Unit = {

    val typeName = c.runtimeClass.getSimpleName.capitalize

    test(s"Secret[$typeName] isValueEquals works properly") {
      forall { (value: T) =>
        val s1 = Secret(value)
        val s2 = Secret(value)

        val c1 = expect(s1.isValueEquals(s2))
        s1.destroy()
        val c2 = expect(!s1.isValueEquals(s2))
        val c3 = expect(!s2.isValueEquals(s1))
        s2.destroy()
        val c4 = expect(!s1.isValueEquals(s2))

        c1 && c2 && c3 && c4
      }
    }

    // use
    test(s"Secret[$typeName] obfuscate and de-obfuscate properly - use") {
      forall { (value: T) =>
        whenSuccess(
          Secret(value)
            .euse[Unit](identity)
        )(result => expect(result == value))
      }
    }
  }
