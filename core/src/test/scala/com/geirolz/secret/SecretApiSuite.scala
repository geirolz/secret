package com.geirolz.secret

import cats.Eq
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyFactory}
import com.geirolz.secret.testing.SecretBuilder
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import weaver.SimpleIOSuite
import weaver.scalacheck.{*, given}

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.util.Try
import cats.Show

/** This is the test suite for SecretApi functionality using various SecretStrategy factories.
  *
  * Secret strategies:
  *   - Xor
  *   - Plain
  *
  * Secret types:
  *   - Secret
  *   - OneShotSecret
  *
  * This test suite tests the following:
  *   - SecretApi.useAndDestroy
  *   - SecretApi.euseAndDestroy
  *   - SecretApi.evalUseAndDestroy
  *   - SecretApi.mapAndDestroy
  *   - SecretApi.flatMapAndDestroy
  *   - SecretApi.asHashedAndDestroy
  *   - SecretApi.hash
  *   - SecretApi.isHashEquals
  *   - SecretApi.isValueEquals
  *   - SecretApi.isDestroyed
  *   - SecretApi.destructionLocation
  */

// xor
object XorSecretApiSuite extends SecretApiSuite(SecretBuilder.secret)(using SecretStrategy.xorFactory)
object XorOneShotSecretApiSuite extends SecretApiSuite(SecretBuilder.secret)(using SecretStrategy.xorFactory)

//plain
object PlainSecretApiSuite extends SecretApiSuite(SecretBuilder.oneShotSecret)(using SecretStrategy.plainFactory)
object OneShotPlainSecretApiSuite extends SecretApiSuite(SecretBuilder.oneShotSecret)(using SecretStrategy.plainFactory)

abstract class SecretApiSuite[S[X] <: SecretApi[X]](sbuilder: SecretBuilder[S])(using SecretStrategyFactory)
    extends SimpleIOSuite
    with Checkers:

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

  pureTest("Simple Secret String") {
    val s1     = sbuilder("TEST")
    val result = s1.euseAndDestroy(_ => ())
    expect(result.isRight)
  }

  pureTest("Simple Secret String destroyed") {
    val s1      = sbuilder("TEST")
    val result1 = s1.euseAndDestroy(_ => ())
    val result2 = s1.euseAndDestroy(_ => ())

    expect(result1.isRight) && expect(result2.isLeft)
  }

  pureTest("Simple Secret with long String") {

    val s1 = sbuilder(
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
    )

    val result = s1.euseAndDestroy(_ => ())
    expect(result.isRight)
  }

  private def testSecretStrategyFor[T: Arbitrary: Eq: SecretStrategy: Show](using c: ClassTag[T]): Unit = {

    val typeName = c.runtimeClass.getSimpleName.capitalize

    test(s"${sbuilder.name}[$typeName] successfully obfuscate") {
      forall { (value: T) =>
        sbuilder(value)
        expect(true)
      }
    }

    test(s"${sbuilder.name}[$typeName] equals always return false") {
      forall { (value: T) =>
        expect(sbuilder(value) != sbuilder(value))
      }
    }

    test(s"${sbuilder.name}[$typeName] hashCode is different from the value one") {
      forall { (value: T) =>
        val secretHashCode = sbuilder(value).hashCode()
        expect(secretHashCode != value.hashCode())
      }
    }

    test(s"${sbuilder.name}[$typeName] obfuscate and de-obfuscate properly - useAndDestroy") {
      forall { (value: T) =>
        val secret: S[T] = sbuilder(value)
        val result1      = secret.euseAndDestroy(identity)
        val result2      = secret.useAndDestroy[Try, Int](_.hashCode())

        whenSuccess(result1)(result => expect(result == value)) &&
        expect(result2.isFailure) &&
        expect(secret.isDestroyed)
      }
    }

    test(s"${sbuilder.name}[$typeName] isValueEquals works properly") {
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
  }
