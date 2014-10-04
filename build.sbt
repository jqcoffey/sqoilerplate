name := "sqoilerplate"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"         % "2.1.2",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.1.2",
  "com.h2database"  %  "h2"                  % "1.4.+",
  "ch.qos.logback"  %  "logback-classic"     % "1.1.+",
  "org.scalatest"   % "scalatest_2.11"       % "2.2.1"  % "test"
)

jacoco.settings

