organization := "com.turkcellteknoloji.iotdb"

version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.10.3"

scalacOptions += "-optimize"

lazy val core = project

lazy val service = project.dependsOn(core)

lazy val rest = project.dependsOn(core,service,cassandrapersistence).settings( webSettings :_*)

lazy val cassandrapersistence = project.dependsOn(core).in(file("cassandra-persistence"))

lazy val jdbcpersistence = project.dependsOn(core).in(file("jdbc-persistence"))

libraryDependencies in ThisBuild ++= Seq("com.github.nscala-time" %% "nscala-time" % "0.6.0")