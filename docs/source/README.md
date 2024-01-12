# Secret
A functional, type-safe and memory-safe class to handle secret values 

`Secret` does the best to avoid leaking information in memory and in the code BUT an attack is possible and I don't give any certainties or
guarantees about security using this class, you use it at your own risk. Code is open source, you can check the implementation and take your
decision consciously. I'll do my best to improve the security and documentation of this class.

Please, drop a ⭐️ if you are interested in this project and you want to support it.


## Obfuscation

The value is obfuscated when creating the `Secret` instance using an implicit `Obfuser`which, by default, transform the value into a xor-ed
`ByteBuffer` witch store bytes outside the JVM using direct memory access.

The obfuscated value is de-obfuscated using an implicit `DeObfuser` instance every time the method `use` is invoked which returns the original
value converting bytes back to `T` re-apply the xor.


## API and Type safety

While obfuscating the value prevents or at least makes it harder to read the value from memory, Secret class API is designed to avoid leaking
information in other ways. Preventing developers to improperly use the secret value ( logging, etc...).

Example
```mdoc scala
  import com.github.geirolz.secret.Secret
  
  val secretString: Secret[String]  = Secret("my_password")
  val database: F[Database]         = secretString.use(password => initDb(password))
```

## Getting Started

To get started with Secret, follow these steps:

1. **Installation:** Include the library as a dependency in your Scala project. You can find the latest version and
   installation instructions in the [Secret GitHub repository](https://github.com/geirolz/secret).

```sbt
libraryDependencies += "com.github.geirolz" %% "secret" % "@VERSION@"
```

## Adopters

If you are using Secret in your company, please let me know and I'll add it to the list! It means a lot to me.

## Contributing

We welcome contributions from the open-source community to make Secret even better. If you have any bug reports,
feature requests, or suggestions, please submit them via GitHub issues. Pull requests are also welcome.

Before contributing, please read
our [Contribution Guidelines](https://github.com/geirolz/secret/blob/main/CONTRIBUTING.md) to understand the
development process and coding conventions.

Please remember te following:

- Run `sbt scalafmtAll` before submitting a PR.
- Run `sbt gen-doc` to update the documentation.

## License

Secret is released under the [Apache License 2.0](https://github.com/geirolz/secret/blob/main/LICENSE).
Feel free to use it in your open-source or commercial projects.

## Acknowledgements
- https://westonal.medium.com/protecting-strings-in-jvm-memory-84c365f8f01c
- VisualVM
- ChatGPT
- Personal experience in companies where I worked
