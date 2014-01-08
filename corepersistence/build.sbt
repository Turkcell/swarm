name := "corepersistence"

libraryDependencies ++= Seq(
  // use the right Slick version here:
  "com.typesafe.slick" %% "slick" % "2.0.0-RC1",
  "com.typesafe" % "config" % "1.0.2",
  "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
  "org.hsqldb" % "hsqldb" % "2.3.1" % "test",
  "org.scalatest" %% "scalatest" % "1.9.2" % "test"
)
