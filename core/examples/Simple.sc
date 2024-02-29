import com.geirolz.secret.Secret

val s1 = Secret("my_password")
s1.hashed

s1.euse(secret => print(secret))
s1.euseAndDestroy(secret => print(secret))
s1.euse(secret => print(secret))


