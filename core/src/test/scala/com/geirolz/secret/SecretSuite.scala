package com.geirolz.secret

import cats.Eq
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyFactory}
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.util.{Failure, Try}

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

  test("Simple Secret String") {
    Secret("TEST").useAndDestroyE(_ => ())
  }

  test("Simple Secret String destroyed") {
    val s1 = Secret("TEST")
    s1.useAndDestroyE(_ => ())
    println(s1.useAndDestroyE(_ => ()))
  }

  test("Option Secret getOrEmptySecret") {

    // some
    val someSecret: Option[Secret[String]] = Option(Secret("TEST"))
    someSecret.getOrEmptySecret.useAndDestroyE { value =>
      assertEquals(
        obtained = value,
        expected = "TEST"
      )
    }

    // none
    val noneSecret: Option[Secret[String]] = None
    noneSecret.getOrEmptySecret.useAndDestroyE { value =>
      assertEquals(
        obtained = value,
        expected = ""
      )
    }
  }

  test("Either Secret getOrEmptySecret") {

    // some
    val rightSecret: Either[String, Secret[String]] = Right(Secret("TEST"))
    rightSecret.getOrEmptySecret.useAndDestroyE { value =>
      assertEquals(
        obtained = value,
        expected = "TEST"
      )
    }

    // none
    val leftSecret: Either[String, Secret[String]] = Left("ERROR")
    leftSecret.getOrEmptySecret.useAndDestroyE { value =>
      assertEquals(
        obtained = value,
        expected = ""
      )
    }
  }

  test("Simple Secret with long String") {
    Secret(
      """|C#iur0#UsxTWzUZ5QPn%KGo$922SMvc5zYLqrcdE6SU6ZpFQrk3&W
         |1c48obb&Rngv9twgMHTuXG@hRb@FZg@u!uPoG%dxTab0QtTab0Qta
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |KGo$922SMvc5zYLqrcdEKGo$922SMvc5zYLqrcdE6SU6ZpFQrk36S
         |U6ZpFQrk31hRbc48obb1c48obb&Rngv9twgMHTuXG@hRb@FZg@u!u
         |PoG%dxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMvc5zY
         |LqrcdE6SdxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMv
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |1c48obb&Rngv9twgMHTuXG@hRb@FZg@u!uPoG%dxTab0QtTab0Qta
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |KGo$922SMvc5zYLqrcdEKGo$922SMvc5zYLqrcdE6SU6ZpFQrk36S
         |U6ZpFQrk31hRbc48obb1c48obb&Rngv9twgMHTuXG@hRb@FZg@u!u
         |PoG%dxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMvc5zY
         |LqrcdE6SdxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMv
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |qrcdEKGo$922SMvc5zYU6ZpFQrk31hRbc48obb1c48obbQrqgk36S
         |PoG%dxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMvc5zY
         |LqrcdE6SdxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMv
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |1c48obb&Rngv9twgMHTuXG@hRb@FZg@u!uPoG%dxTab0QtTab0Qta
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |KGo$922SMvc5zYLqrcdEKGo$922SMvc5zYLqrcdE6SU6ZpFQrk36S
         |U6ZpFQrk31hRbc48obb1c48obb&Rngv9twgMHTuXG@hRb@FZg@u!u
         |PoG%dxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMvc5zY
         |LqrcdE6SdxTab0QtTab0QtaKGo$922SMvc5zYLqrcdEKGo$922SMv
         |c5zYU6ZpRngv9twgMHTuXGFdxTab0QtTab0QtaKGo$922SMvc5zYL
         |qrcdEKGo$922SMvc5zYU6ZpFQrk31hRbc48obb1c48obbQrqgk36S
         |qrcdEKGo$922SMvc5zYU6ZpFQrk31hRbc48obb1c48obbQrqgk36S
         |""".stripMargin
    ).useAndDestroyE(_ => ())
  }

  private def testSecretStrategyFor[T: Arbitrary: Eq: SecretStrategy](using c: ClassTag[T]): Unit = {

    val typeName = c.runtimeClass.getSimpleName.capitalize

    property(s"Secret[$typeName] successfully obfuscate") {
      forAll { (value: T) =>
        Secret(value)
        assert(cond = true)
      }
    }

    property(s"Secret[$typeName] equals always return false") {
      forAll { (value: T) =>
        assertNotEquals(Secret(value), Secret(value))
      }
    }

    property(s"Secret[$typeName] isEquals works properly") {
      forAll { (value: T) =>
        val s1 = Secret(value)
        val s2 = Secret(value)

        assert(s1.isEquals(s2))
        s1.destroy()
        assert(!s1.isEquals(s2))
        assert(!s2.isEquals(s1))
        s2.destroy()
        assert(!s1.isEquals(s2))
      }
    }

    property(s"Secret[$typeName] hashCode is different from the value one") {
      forAll { (value: T) =>
        assert(Secret(value).hashCode() != value.hashCode())
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

    // useAndDestroy
    property(s"Secret[$typeName] obfuscate and de-obfuscate properly - useAndDestroy") {
      forAll { (value: T) =>
        val secret: Secret[T] = Secret(value)

        assert(
          secret
            .useAndDestroy[Try, Unit] { result =>
              assertEquals(
                obtained = result,
                expected = value
              )
            }
            .isSuccess
        )

        assertEquals(
          obtained = secret.useAndDestroy[Try, Int](_.hashCode()).isFailure,
          expected = true
        )
        assertEquals(
          obtained = secret.isDestroyed,
          expected = true
        )
      }
    }
  }
