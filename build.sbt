name := "Free Cats"

version := "1.0"

scalaVersion := "2.12.6"

libraryDependencies += "org.typelevel" %% "cats-core" % "1.4.0"
libraryDependencies += "org.typelevel" %% "cats-free" % "1.4.0"

libraryDependencies += "io.monix" %% "monix" % "3.0.0-M3"

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-language:postfixOps",
  "-deprecation",
  "-Xfatal-warnings",
  "-Ywarn-unused:imports",
  "-Ypartial-unification"
)

