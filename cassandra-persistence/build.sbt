name := "cassandra-persistence"

libraryDependencies ++= Seq(
	"com.datastax.cassandra"  % "cassandra-driver-core" % "2.0.0-rc1",
	"com.typesafe" % "config" % "1.0.2",
	"org.scalatest" %% "scalatest" % "1.9.2" % "test"
)  

EclipseKeys.withSource := true