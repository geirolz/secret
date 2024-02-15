package com.geirolz.secret.util

import cats.Show

import scala.quoted.{Expr, Quotes}
import scala.quoted.*

class Location(
  val sourceFile: String,
  val line: Int,
  val column: Int,
  val sourceCode: Option[String]
):
  override def toString: String = s"$sourceFile:$line:$column"

object Location:

  inline given Location = ${ currentLocationMacro }
  private def currentLocationMacro(using Quotes): Expr[Location] =
    val position                         = quotes.reflect.Position.ofMacroExpansion
    val sourceFile: Expr[String]         = Expr(position.sourceFile.toString)
    val line: Expr[Int]                  = Expr(position.endLine + 1)
    val column: Expr[Int]                = Expr(position.startColumn)
    val sourceCode: Expr[Option[String]] = Expr(position.sourceCode)

    '{ new Location($sourceFile, $line, $column, $sourceCode) }

  given Show[Location] = Show.fromToString
