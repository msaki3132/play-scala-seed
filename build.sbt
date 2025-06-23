name := """play-scala-seed"""
organization := "com.monoxer"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.16"

lazy val admin = (project in file("modules/admin")).enablePlugins(PlayScala)
lazy val v2 = (project in file("modules/v2")).enablePlugins(PlayScala)
lazy val core = (project in file("modules/core")).enablePlugins(PlayScala)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(PackPlugin)
  .enablePlugins(SwaggerPlugin)
  .dependsOn(admin, v2, core)
  .aggregate(admin, v2, core)
  .settings(
    swaggerTarget := file("docs")
  )


libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.monoxer.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.monoxer.binders._"
