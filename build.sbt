val scala3Version = "3.5.1"
val http4sVersion = "0.23.28"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "scuffedb",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.http4s"                  %% "http4s-ember-client" % http4sVersion,
      "org.http4s"                  %% "http4s-ember-server" % http4sVersion,
      "org.http4s"                  %% "http4s-dsl"          % http4sVersion,
      "org.typelevel"               %% "cats-core"           % "2.12.0",
      ("com.github.alexandrnikitin" %% "bloom-filter"        % "0.13.1")
        .cross(CrossVersion.for3Use2_13)
        .exclude("org.typelevel", "cats-kernel_2.13"),
      "org.scalameta" %% "munit"             % "1.0.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
    )
  )
