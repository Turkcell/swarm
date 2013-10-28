name := "server"

libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % "2.2.1",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.3",
        "org.scalatra" %% "scalatra-json" % "2.2.1",
  		"org.json4s"   %% "json4s-jackson" % "3.2.4",
        "org.scalatra" %% "scalatra-specs2" % "2.2.1" % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.11" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.10.v20130312" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
      )