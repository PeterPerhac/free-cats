name := "Free Cats"

version := "1.0"

scalaVersion := "2.12.6"
lazy val catsVersion = "1.6.0"
lazy val doobieVersion = "0.5.3"
lazy val monixVersion = "3.0.0-M3"


mainClass in (Compile, run) := Some("com.perhac.experiments.cats.free.CheckWeather")

libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion
libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion

libraryDependencies += "org.tpolecat" %% "doobie-core"     % doobieVersion
libraryDependencies += "org.tpolecat" %% "doobie-postgres" % doobieVersion

libraryDependencies += "io.monix" %% "monix" % monixVersion

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-language:postfixOps",
  "-deprecation",
  "-Xfatal-warnings",
  "-Ypartial-unification"
)

