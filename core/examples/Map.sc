import com.geirolz.secret.Secret


val s1: Secret[String] = Secret("A")
val s2: Secret[String] = s1.map(_ + "B")
val s3: Secret[String] = s2.map(_ + "C")

s3.useAndDestroyE(println(_))

s1.isDestroyed
s2.isDestroyed
s3.isDestroyed
