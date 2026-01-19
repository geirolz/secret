package com.geirolz.secret.transform

import cats.effect.IO
import com.geirolz.secret.*
import weaver.SimpleIOSuite

import java.nio.charset.StandardCharsets
import scala.util.Try

class HasherSecretSuite extends SimpleIOSuite:

  // MD
  testHasher("md2")(
    hasher   = Hasher.md2,
    plain    = "Hello World",
    expected = "27454d000b8f9aaa97da6de8b394d986"
  )

  testHasher("md5")(
    hasher   = Hasher.md5,
    plain    = "Hello World",
    expected = "b10a8db164e0754105b7a99be72e3fe5"
  )

  // SHA
  // sha1
  testHasher("sha1")(
    hasher   = Hasher.sha1,
    plain    = "Hello World",
    expected = "0a4d55a8d778e5022fab701977c5d840bbc486d0"
  )

  // sha2
  testHasher("sha256")(
    hasher   = Hasher.sha256,
    plain    = "Hello World",
    expected = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"
  )

  testHasher("sha384")(
    hasher   = Hasher.sha384,
    plain    = "Hello World",
    expected = "99514329186b2f6ae4a1329e7ee6c610a729636335174ac6b740f9028396fcc803d0e93863a7c3d90f86beee782f4f3f"
  )

  testHasher("sha512")(
    hasher = Hasher.sha512,
    plain  = "Hello World",
    expected =
      "2c74fd17edafd80e8447b0d46741ee243b7eb74dd2149a0ab1b9246fb30382f27e853d8585719e0e67cbda0daa8f51671064615d645ae27acb15bfb1447f459b"
  )

  // sha3
  testHasher("sha3-256")(
    hasher   = Hasher.sha3_256,
    plain    = "Hello World",
    expected = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f51"
  )

  testHasher("sha3-384")(
    hasher   = Hasher.sha3_384,
    plain    = "Hello World",
    expected = "a78ec2851e991638ce505d4a44efa606dd4056d3ab274ec6fdbac00cde16478263ef7213bad5a7db7044f58d637afdeb"
  )

  testHasher("sha3-512")(
    hasher = Hasher.sha3_512,
    plain  = "Hello World",
    expected =
      "3d58a719c6866b0214f96b0a67b37e51a91e233ce0be126a08f35fdf4c043c6126f40139bfbc338d44eb2a03de9f7bb8eff0ac260b3629811e389a5fbee8a894"
  )

  inline def testHasher(
    title: String
  )(hasher: Try[Hasher], plain: String, expected: String): Unit =
    test(title) {
      IO.fromEither(
        Secret(plain)
          .map(v => hasher.get.hashAsString(v.getBytes(StandardCharsets.UTF_8)))
          .euse(v => expect(v == expected))
      )
    }
