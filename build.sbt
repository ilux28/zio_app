ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "3.3.5"


lazy val root = (project in file("."))
  .settings(
    name := "zio_app"
  )
lazy val zioVersion = "2.1.15"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion
)
