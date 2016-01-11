import sbt.Keys._

name := "leader-election-algorithms"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  Seq(
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test" withSources(),
    "org.scala-lang.modules" % "scala-async_2.11" % "0.9.4" withSources(),
    "com.typesafe.akka" %% "akka-actor" % "2.4.1" withSources(),
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "org.slf4j" % "slf4j-api" % "1.7.12",
    "ch.qos.logback" % "logback-classic" % "1.1.3"
  )
}

