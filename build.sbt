val scala3Version = "3.5.1"
val http4sVersion = "0.23.28"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "keyvalDb",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl"          % http4sVersion
    ),
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    libraryDependencies += "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
  )
