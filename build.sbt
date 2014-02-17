organization := "io.swarm"

version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.10.3"

lazy val core = project

lazy val tsdb = project.dependsOn(core)

lazy val cassandrapersistence = project.dependsOn(core)

lazy val corepersistence = project.dependsOn(core)

lazy val tsdbmetadatapersistence=project.dependsOn(tsdb,corepersistence)

lazy val coreservice = project.dependsOn(core,corepersistence)

lazy val rest = project.dependsOn(core,coreservice,cassandrapersistence).settings( webSettings :_*)

libraryDependencies in ThisBuild ++= Seq(
    "com.github.nscala-time" %% "nscala-time" % "0.6.0",
    "org.scalatest" %% "scalatest" % "2.0" % "test",
    "ch.qos.logback" % "logback-classic" % "1.0.11" % "runtime"
    )