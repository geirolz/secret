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

// xor
class XorSecretApiSuite extends SecretApiSuite(SecretBuilder.secret)(using SecretStrategy.xorFactory)
class XorOneShotSecretApiSuite extends SecretApiSuite(SecretBuilder.secret)(using SecretStrategy.xorFactory)

//plain
class PlainSecretApiSuite extends SecretApiSuite(SecretBuilder.oneShotSecret)(using SecretStrategy.plainFactory)
class OneShotPlainSecretApiSuite extends SecretApiSuite(SecretBuilder.oneShotSecret)(using SecretStrategy.plainFactory)

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
    val s1 = sbuilder("TEST")
    expect(s1.euseAndDestroy(_ => ()).isRight)
  }

  pureTest("Simple Secret String destroyed") {
    val s1 = sbuilder("TEST")
    expect(s1.euseAndDestroy(_ => ()).isRight) &&
    expect(s1.euseAndDestroy(_ => ()).isLeft)
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

    expect(s1.euseAndDestroy(_ => ()).isRight)
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
        expect(sbuilder(value).hashCode() != value.hashCode())
      }
    }

    test(s"${sbuilder.name}[$typeName] obfuscate and de-obfuscate properly - useAndDestroy") {
      forall { (value: T) =>
        val secret: S[T] = sbuilder(value)

        whenSuccess(secret.euseAndDestroy[Unit](identity))(result => expect(result == value)) &&
        expect(secret.useAndDestroy[Try, Int](_.hashCode()).isFailure) &&
        expect(secret.isDestroyed)
      }
    }
  }
