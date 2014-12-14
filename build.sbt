import sbt._
import sbt.Keys._

organizationName := "Jeff May"

organization := "me.jeffmay"

name := "bson-adt"

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

sources in(Compile, doc) := Seq.empty

// enable publishing the jar produced by `test:package`
publishArtifact in(Test, packageBin) := true

// enable publishing the test sources jar
publishArtifact in(Test, packageSrc) := true

scalacOptions ++= Seq("-Xfatal-warnings", "-feature")
