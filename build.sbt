import sbt._
import sbt.Keys._

organizationName := "Jeff May"

organization := "me.jeffmay"

name := "bson-adt"

version := "1.1.1"

// TODO: Cross-compile with 2.11
scalaVersion := "2.10.4"

lazy val casbahVersion = SettingKey[String]("casbahVersion")

casbahVersion := "2.6.2"

libraryDependencies ++= Seq(
  "org.mongodb" %% "casbah-query" % casbahVersion.value,
  "org.mongodb" %% "casbah-core" % casbahVersion.value,
  "org.mongodb" % "mongo-java-driver" % "2.11.2",
  "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
  "org.scalatest" %% "scalatest" % "2.1.0" % "test",
  "com.typesafe.play" %% "play-functional" % "2.4.0-M2"
)

// disable compilation of ScalaDocs, since this always breaks on links
sources in(Compile, doc) := Seq.empty

// disable publishing empty ScalaDocs
publishArtifact in (Compile, packageDoc) := false

// enable publishing the jar produced by `test:package`
publishArtifact in(Test, packageBin) := true

// enable publishing the test sources jar
publishArtifact in(Test, packageSrc) := true

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-dead-code",
  "-encoding", "UTF-8"
)

bintraySettings ++ bintrayPublishSettings

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache-2.0"))
