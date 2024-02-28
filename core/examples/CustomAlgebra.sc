import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyAlgebra}
import com.geirolz.secret.util.KeyValueBuffer
import com.geirolz.secret.{PlainValueBuffer, Secret}

import java.nio.ByteBuffer

// build the custom algebra
val myCustomAlgebra = new SecretStrategyAlgebra:
  final def obfuscator[P](f: P => PlainValueBuffer): Obfuscator[P] =
    Obfuscator.of { (plain: P) => KeyValueBuffer(ByteBuffer.allocateDirect(0), f(plain)) }

  final def deObfuscator[P](f: PlainValueBuffer => P): DeObfuscator[P] =
    DeObfuscator.of { bufferTuple => f(bufferTuple.roObfuscatedBuffer) }

// build factory base on the algebra
val myCustomStrategyFactory = myCustomAlgebra.newFactory

// implicitly in the scope
import myCustomStrategyFactory.given
Secret("my_password").euse(secret => secret)

// or restricted to a specific scope
myCustomStrategyFactory {
  Secret("my_password").euse(secret => secret)
}
