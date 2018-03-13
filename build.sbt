name := "Simple Project"

version := "1.0"

scalaVersion := "2.11.11"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.2.1" //% "provided"
//libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.2.1"
//libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-8" % "2.2.1"
//libraryDependencies += "org.apache.kafka" %% "kafka" % "0.8.2.1"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.2.1"

