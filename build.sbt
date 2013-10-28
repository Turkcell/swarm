organization := "com.turkcellteknoloji.iotdb"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions += "-optimize"

lazy val domain = project

lazy val server = project.dependsOn(domain,cassandrapersistence).settings( webSettings :_*)

lazy val cassandrapersistence = project.dependsOn(domain).in(file("cassandra-persistence"))

lazy val jdbcpersistence = project.dependsOn(domain).in(file("jdbc-persistence"))