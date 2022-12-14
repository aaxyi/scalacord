ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization     := "dev.axyria"
ThisBuild / organizationName := "FromSyntax"
ThisBuild / startYear        := Some(2022)
ThisBuild / licenses         := Seq(License.MIT)
ThisBuild / developers       := List(tlGitHubDev("FromSyntax", "Pedro Henrique"))

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / scalaVersion                                   := "3.2.1"
ThisBuild / semanticdbEnabled                              := true
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

lazy val deps = new {
    val typelevel = Seq(
        libraryDependencies ++= Seq(
            "org.typelevel" %%% "cats-effect" % "3.3.14",
            "org.typelevel" %%% "cats-core"   % "2.8.0",
            "org.typelevel" %%% "spire"       % "0.18.0"
        )
    )
    val stream   = Seq(libraryDependencies ++= Seq("co.fs2" %%% "fs2-core" % "3.3.0"))
    val streamIo = Seq(libraryDependencies ++= Seq("co.fs2" %%% "fs2-io" % "3.3.0"))
    val http = Seq(
        libraryDependencies ++= Seq("client", "circe").map(a =>
            "org.http4s" %%% ("http4s-" ++ a) % "0.23.16"
        )
    )
    val httpJvm = Seq(
        libraryDependencies ++= Seq(
            "org.http4s" %% "http4s-jdk-http-client" % "0.7.0",
        )
    )
    val httpJs = Seq(
        libraryDependencies ++= Seq(
            "org.http4s" %%% "http4s-dom" % "0.2.3",
        )
    )
    val test = Seq(
        libraryDependencies ++= Seq(
            "org.scalameta" %%% "munit"             % "1.0.0-M6" % Test,
            "org.typelevel" %%% "munit-cats-effect" % "2.0.0-M3" % Test,
        )
    )
    val json = Seq(
        libraryDependencies ++= Seq(
            "core",
            "generic",
            "parser",
        ).map(a => "io.circe" %%% ("circe-" ++ a) % "0.14.3")
    )
    val logging = Seq(libraryDependencies ++= Seq("org.typelevel" %%% "log4cats-core" % "2.5.0"))
}

lazy val common = crossProject(JVMPlatform, NativePlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("common"))
    .settings(name := "scalacord-common", semanticdbEnabled := true)
    .settings(deps.typelevel ++ deps.json ++ deps.logging ++ deps.test)

lazy val rest = crossProject(JVMPlatform, NativePlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("rest"))
    .settings(name := "scalacord-rest", semanticdbEnabled := true)
    .settings(deps.typelevel ++ deps.json ++ deps.logging ++ deps.stream ++ deps.http ++ deps.test)
    .dependsOn(common)

lazy val gateway = crossProject(JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("gateway"))
    .settings(name := "scalacord-gateway", semanticdbEnabled := true)
    .settings(
        deps.typelevel ++ deps.json ++ deps.logging ++ deps.stream ++ deps.streamIo ++ deps.http ++ deps.test
    )
    .jvmSettings(deps.httpJvm)
    // .jsSettings(deps.httpJs)
    .dependsOn(common)

lazy val core = crossProject(JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("core"))
    .settings(name := "scalacord-core", semanticdbEnabled := true)
    .aggregate(common, rest, gateway)
    .dependsOn(common, rest, gateway)

lazy val root = tlCrossRootProject.aggregate(core)
