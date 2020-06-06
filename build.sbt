name := "tapir-test"

version := "0.1"

scalaVersion := "2.13.2"

val http4sVersion = "0.21.3"
val tapirVersion = "0.15.3"
val catsVersions = "2.1.1"
val circeVersion = "0.13.0"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe"    % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,

  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,

  // fp
  "org.typelevel" %% "cats-core"   % catsVersions,
  "org.typelevel" %% "cats-macros" % catsVersions,
  "org.typelevel" %% "cats-free"   % catsVersions,
  "org.typelevel" %% "cats-effect" % catsVersions,

  //circe
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

