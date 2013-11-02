name := "core"

libraryDependencies ++= Seq(
    "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
    "org.apache.shiro" % "shiro-core" % "1.2.2",
    "commons-codec" % "commons-codec" % "1.4"
)

EclipseKeys.withSource := true
