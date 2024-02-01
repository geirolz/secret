import com.geirolz.secret.Secret


val s1: Secret[String] = Secret("A")
val s2: Secret[String] = Secret("B")
val s3: Secret[String] = Secret("C")
val result: Secret[String] = for {
  v1 <- s1
  v2 <- s2
  v3 <- s3
} yield (v1 + v2 + v3)


s1.isDestroyed
s2.isDestroyed
s3.isDestroyed
result.isDestroyed

result.destroy()

s1.isDestroyed
s2.isDestroyed
s3.isDestroyed
result.isDestroyed


