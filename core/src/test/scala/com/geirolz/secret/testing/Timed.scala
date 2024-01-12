package com.geirolz.secret.testing

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object Timed {
  def apply[T](f: => T): (FiniteDuration, T) = {
    val start    = System.nanoTime()
    val result   = f
    val end      = System.nanoTime()
    val duration = FiniteDuration(end - start, TimeUnit.NANOSECONDS)
    (duration, result)
  }
}
