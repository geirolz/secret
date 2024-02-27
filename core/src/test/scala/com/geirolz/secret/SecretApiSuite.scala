package com.geirolz.secret

import cats.Eq
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyFactory}
import com.geirolz.secret.testing.SecretBuilder
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.util.Try

// xor
class XorSecretApiSuite extends SecretApiSuite(SecretBuilder.secret)(using SecretStrategy.xorFactory)
class XorOneShotSecretApiSuite extends SecretApiSuite(SecretBuilder.secret)(using SecretStrategy.xorFactory)

//plain
class PlainSecretApiSuite extends SecretApiSuite(SecretBuilder.oneShotSecret)(using SecretStrategy.plainFactory)
class OneShotPlainSecretApiSuite extends SecretApiSuite(SecretBuilder.oneShotSecret)(using SecretStrategy.plainFactory)

abstract class SecretApiSuite[S[X] <: SecretApi[X]](b: SecretBuilder[S])(using SecretStrategyFactory)
    extends munit.ScalaCheckSuite:

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
    b("TEST").euseAndDestroy(_ => ())
  }

  test("Simple Secret String destroyed") {
    val s1 = b("TEST")
    s1.euseAndDestroy(_ => ())
  }

  test("Simple Secret with long String") {
    b(
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
    ).euseAndDestroy(_ => ())
  }

  private def testSecretStrategyFor[T: Arbitrary: Eq: SecretStrategy](using c: ClassTag[T]): Unit = {

    val typeName = c.runtimeClass.getSimpleName.capitalize

    property(s"${b.name}[$typeName] successfully obfuscate") {
      forAll { (value: T) =>
        b(value)
        assert(cond = true)
      }
    }

    property(s"${b.name}[$typeName] equals always return false") {
      forAll { (value: T) =>
        assertNotEquals(b(value), b(value))
      }
    }

    property(s"${b.name}[$typeName] hashCode is different from the value one") {
      forAll { (value: T) =>
        assert(b(value).hashCode() != value.hashCode())
      }
    }

    property(s"${b.name}[$typeName] obfuscate and de-obfuscate properly - useAndDestroy") {
      forAll { (value: T) =>
        val secret: S[T] = b(value)

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
