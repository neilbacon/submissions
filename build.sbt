organization := "data61.csiro.au"

scalaVersion := "2.12.2"

version      := "0.1"

name := "submissions"

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil // only Scala sources, no Java

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil
 
com.github.retronym.SbtOneJar.oneJarSettings

mainClass in Compile := Some("au.csiro.data61.submissions.Main")

exportJars := true // required by sbt-onejar

EclipseKeys.withSource := true

// If Eclipse and sbt are both building to same dirs at same time it takes forever and produces corrupted builds.
// So here we tell Eclipse to build somewhere else (bin is it's default build output folder)
EclipseKeys.eclipseOutput in Compile := Some("bin")   // default is sbt's target/scala-2.12/classes

EclipseKeys.eclipseOutput in Test := Some("test-bin") // default is sbt's target/scala-2.12/test-classes

// use a local build of the latest maui snapshot
// I got errors from jacoco-maven-plugin (test code coverage tool), so commented that out in the maui/pom.xml
// install to ~/.m2/repository with: mvn install
// Following tells sbt to look there:
resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
    "org.jsoup" % "jsoup" % "1.10.2",
    "org.apache.pdfbox" % "pdfbox" % "2.0.5" exclude("commons-logging", "commons-logging"), // instead using jcl-over-slf4j
    "com.entopix" % "maui" % "1.3.1-SNAPSHOT" exclude("org.slf4j", "slf4j-log4j12"),
    // "org.http4s" %% "http4s-blaze-client" % "0.15.11",
    // "org.http4s" %% "http4s-argonaut" % "0.15.11",
    "com.github.scopt" % "scopt_2.12" % "3.5.0",
    "com.jsuereth" %% "scala-arm" % "2.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.2.2",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.25"
)
