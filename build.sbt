name := "id2221-final-project"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.4"
libraryDependencies += "com.github.fsanaulla" %% "chronicler-spark-streaming" % "0.3.1"
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.4"
libraryDependencies += "org.json4s" %% "json4s-core" % "3.6.7"