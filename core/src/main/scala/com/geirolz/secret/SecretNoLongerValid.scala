package com.geirolz.secret

import scala.util.control.NoStackTrace

case class SecretNoLongerValid() extends RuntimeException("This secret value is no longer valid") with NoStackTrace
