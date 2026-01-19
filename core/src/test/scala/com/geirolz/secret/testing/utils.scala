package com.geirolz.secret.testing

import cats.Applicative
import cats.effect.IO
import cats.syntax.all.*
import com.eed3si9n.expecty.RecorderMacro
import weaver.{Expect, Expectations}

extension (expect: Expect)
  inline def allF[F[_]: Applicative](inline recordings: F[Expectations]*): F[Expectations] =
    recordings.sequence.map(_.reduce(_ && _))
