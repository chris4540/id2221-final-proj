name := "id2221-final-project"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.4" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "com.paulgoldbaum" %% "scala-influxdb-client" % "0.6.1" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.4" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.json4s" %% "json4s-core" % "3.6.7" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
