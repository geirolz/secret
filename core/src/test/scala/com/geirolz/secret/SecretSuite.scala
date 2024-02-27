package com.geirolz.secret

import cats.Eq
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyFactory}
import com.geirolz.secret.util.{Hasher, SysEnv}
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.util.Try

class XorSecretSuite extends SecretSuite(using SecretStrategy.xorFactory)
class PlainSecretSuite extends SecretSuite(using SecretStrategy.plainFactory)

abstract class SecretSuite(using SecretStrategyFactory) extends munit.ScalaCheckSuite:

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
    given SysEnv[Try] = SysEnv.fromMap[Try](Map("TEST" -> "VALUE"))
    Secret
      .fromEnv[Try]("TEST")
      .get
      .euseAndDestroy(value =>
        assertEquals(
          obtained = value,
          expected = "VALUE"
        )
      )
  }

  test("Secret.deferredFromEnv") {
    given SysEnv[Try] = SysEnv.fromMap[Try](Map("TEST" -> "VALUE"))
    Secret
      .deferFromEnv[Try]("TEST")
      .use(value =>
        assertEquals(
          obtained = value,
          expected = "VALUE"
        )
      )
  }

  test("Option Secret getOrEmptySecret") {

    // some
    val someSecret: Option[Secret[String]] = Option(Secret("TEST"))
    someSecret.getOrEmptySecret.euseAndDestroy { value =>
      assertEquals(
        obtained = value,
        expected = "TEST"
      )
    }

    // none
    val noneSecret: Option[Secret[String]] = None
    noneSecret.getOrEmptySecret.euseAndDestroy { value =>
      assertEquals(
        obtained = value,
        expected = ""
      )
    }
  }

  test("Either Secret getOrEmptySecret") {

    // some
    val rightSecret: Either[String, Secret[String]] = Right(Secret("TEST"))
    rightSecret.getOrEmptySecret.euseAndDestroy { value =>
      assertEquals(
        obtained = value,
        expected = "TEST"
      )
    }

    // none
    val leftSecret: Either[String, Secret[String]] = Left("ERROR")
    leftSecret.getOrEmptySecret.euseAndDestroy { value =>
      assertEquals(
        obtained = value,
        expected = ""
      )
    }
  }

  private def testSecretStrategyFor[T: Arbitrary: Eq: SecretStrategy](using c: ClassTag[T]): Unit = {

    val typeName = c.runtimeClass.getSimpleName.capitalize

    property(s"Secret[$typeName] isEquals works properly") {
      forAll { (value: T) =>
        val s1 = Secret(value)
        val s2 = Secret(value)

        assert(s1.isValueEquals(s2))
        s1.destroy()
        assert(!s1.isValueEquals(s2))
        assert(!s2.isValueEquals(s1))
        s2.destroy()
        assert(!s1.isValueEquals(s2))
      }
    }

    // use
    property(s"Secret[$typeName] obfuscate and de-obfuscate properly - use") {
      forAll { (value: T) =>
        assert(
          Secret(value)
            .use[Try, Unit](result => {
              assertEquals(
                obtained = result,
                expected = value
              )
            })
            .isSuccess
        )
      }
    }
  }
