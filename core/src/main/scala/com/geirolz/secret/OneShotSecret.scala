package com.geirolz.secret

import com.geirolz.secret.internal.{SecretApi, SecretCompanionApi, Vault}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.{Hasher, Location}

/** A `OneShotSecret` is a secret that can be used only once.
  *
  * After the first use, the secret is destroyed.
  *
  * @tparam T
  *   type of the secret
  */
final class OneShotSecret[T] private[secret] (vault: Vault[T]) extends SecretApi[T](vault)
object OneShotSecret extends SecretCompanionApi[OneShotSecret]:

  /** Create a destroyed secret */
  override def destroyed[T](location: Location = Location.unknown): OneShotSecret[T] =
    new OneShotSecret[T](Vault.destroyed[T](location))

  /** Create a new `OneShotSecret` with the specified value.
    *
    * @param value
    *   the value to obfuscate
    * @param collectDestructionLocation
    *   if `true` the location where the secret was destroyed will be collected
    * @return
    *   a new `OneShotSecret`
    */
  override def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): OneShotSecret[T] = new OneShotSecret(Vault[T](value, collectDestructionLocation))
