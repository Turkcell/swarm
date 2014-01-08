name := "core"

libraryDependencies ++= Seq(
  // use the right Slick version here:
  "com.typesafe.slick" %% "slick" % "2.0.0-RC1",
  "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
  "com.typesafe" % "config" % "1.0.2",
  "org.apache.shiro" % "shiro-core" % "1.2.2",
  "commons-codec" % "commons-codec" % "1.4"
)
