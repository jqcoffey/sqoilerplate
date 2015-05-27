name := "sqoilerplate"

version := "0.0.1"

organization := "sqoilerplate"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"         % "2.2.6",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.2.6",
  "ch.qos.logback"  %  "logback-classic"     % "1.1.+",
  "com.h2database"  %  "h2"                  % "1.4.+"  % "test",
  "org.scalatest"   %% "scalatest"           % "2.2.1"  % "test"
)

jacoco.settings

