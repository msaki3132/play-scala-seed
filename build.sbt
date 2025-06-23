name := """play-scala-seed"""
organization := "com.monoxer"

version := "1.0-SNAPSHOT"

//scalaVersion := "2.13.16"
scalaVersion := "3.3.3"

lazy val admin = (project in file("modules/admin")).enablePlugins(PlayScala)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(PackPlugin)
  .enablePlugins(SwaggerPlugin)
  .dependsOn(admin)
  .aggregate(admin)
  .settings(
    swaggerTarget := file("docs")
  )


libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test

// Google Cloud Bigtable dependencies
libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-bigtable" % "2.39.0",
  "com.google.cloud" % "google-cloud-bigtable-emulator" % "0.164.0" % Test,
  "com.typesafe" % "config" % "1.4.3"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.monoxer.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.monoxer.binders._"
