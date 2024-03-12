package com.geirolz.secret

import cats.Show
import com.geirolz.secret.util.Location

import scala.util.control.NoStackTrace

case class SecretDestroyed(destructionLocation: Location)
    extends RuntimeException(s"This secret has been already destroyed here: $destructionLocation")

object SecretDestroyed:
  given Show[SecretDestroyed] = Show(_.getMessage)
