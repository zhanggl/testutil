name := "testutil"

organization := "com.bne"

version := "1.0.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
	"com.twitter" %% "finagle-memcached" % "6.11.1"
)

resolvers +=
  "Twitter" at "http://maven.twttr.com"

