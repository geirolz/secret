package com.geirolz.secret

import com.geirolz.secret.internal.{SecretApi, SecretCompanionApi, Vault}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import com.geirolz.secret.util.Location

/** A `OneShotSecret` is a secret that can be used only once.
  *
  * After the first use, the secret is destroyed.
  *
  * @tparam T
  *   type of the secret
  */
final class OneShotSecret[T] private[secret] (vault: Vault[T]) extends SecretApi[T](vault):
  override type Self[X] = OneShotSecret[X]
  private[secret] override val companion: SecretCompanionApi[Self] = OneShotSecret

object OneShotSecret extends SecretCompanionApi[Secret.OneShot]:

  private[secret] given SecretCompanionApi[Secret.OneShot] = this

  private[secret] def fromVault[T](vault: Vault[T]): Secret.OneShot[T] =
    new OneShotSecret(vault)

  /** Create a destroyed secret */
  override def destroyed[T](location: Location = Location.unknown): Secret.OneShot[T] =
    fromVault[T](Vault.destroyed[T](location))

  /** Create a new `OneShotSecret` with the specified value.
    *
    * @param value
    *   the value to obfuscate
    * @param recDestructionLocation
    *   if `true` the location where the secret was destroyed will be collected
    * @return
    *   a new `OneShotSecret`
    */
  override def apply[T](value: => T, recDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): Secret.OneShot[T] = fromVault(Vault[T](value, recDestructionLocation))
