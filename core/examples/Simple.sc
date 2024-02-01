import com.geirolz.secret.Secret

val s1 = Secret("my_password")
s1.useE(secret => print(secret))
s1.useAndDestroyE(secret => print(secret))
s1.useE(secret => print(secret))

s1.unsafeUse(print(_))

