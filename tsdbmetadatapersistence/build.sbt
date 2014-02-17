name := "tsdbmetadatapersistence"

libraryDependencies ++= Seq(
  // use the right Slick version here:
  "com.typesafe.slick" %% "slick" % "2.0.0-RC1",
  "org.hsqldb" % "hsqldb" % "2.3.1" % "test",
  "org.slf4j" % "slf4j-api" % "1.7.5"
)