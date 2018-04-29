organization in ThisBuild := "com.walcron"
version in ThisBuild := "1.0-SNAPSHOT"

/**Configure Kafka and Cassandra to point to external, but this only works in DEV environment**/
//lagomKafkaEnabled in ThisBuild := false
//lagomKafkaAddress in ThisBuild := "192.168.1.245:9092"
//lagomCassandraEnabled in ThisBuild := false
//lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "http://localhost:9042")

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `justice-league-lagom` = (project in file(".")).aggregate(`justice-league-impl`, `justice-league-api`, `avengers-impl`, `avengers-api`)

lazy val `justice-league-api` = (project in file("justice-league-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `justice-league-impl` = (project in file("justice-league-impl"))
  .enablePlugins(LagomScala)
  .settings(
     libraryDependencies ++= Seq(
       lagomScaladslPersistenceCassandra,
       macwire
     )
   )
  .dependsOn(`justice-league-api`)

lazy val `avengers-api` = (project in file("avengers-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `avengers-impl` = (project in file("avengers-impl"))
  .enablePlugins(LagomScala)
  .settings(
     libraryDependencies ++= Seq(
       lagomScaladslPersistenceCassandra,
       lagomScaladslPubSub,
       lagomScaladslKafkaBroker,
       macwire
     )
   )
  .dependsOn(`avengers-api`)
