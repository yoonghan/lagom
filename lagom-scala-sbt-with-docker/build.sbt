organization in ThisBuild := "com.walcron"
version in ThisBuild := "1.0-SNAPSHOT"

//Configure kafka
lagomKafkaPort in ThisBuild := 10000
lagomKafkaZookeeperPort in ThisBuild := 9999
lagomKafkaAddress in ThisBuild := "localhost:10000"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test


lazy val `justice-league-lagom` = (project in file(".")).aggregate(`justice-league-impl`, `justice-league-api`)

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

