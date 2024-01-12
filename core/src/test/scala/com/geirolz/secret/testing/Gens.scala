package com.geirolz.secret.testing

import org.scalacheck.Gen

object Gens {

  def strGen(size: Int): Gen[String] = Gen.listOfN(size, Gen.alphaChar).map(_.mkString)
}
