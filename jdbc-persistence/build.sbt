name := "jdbc-persistence"

libraryDependencies ++= Seq(
  // use the right Slick version here:
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "com.typesafe" % "config" % "1.0.2",
  "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
  "ch.qos.logback" % "logback-classic" % "1.0.11" % "runtime",
  "org.hsqldb" % "hsqldb" % "2.3.1" % "test",
  "org.scalatest" %% "scalatest" % "1.9.2" % "test",
  "junit" % "junit" % "4.11" % "test"
)


EclipseKeys.withSource := true