import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import org.apache.log4j.LogManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


object AntriebsstrangStreaming {

  def main(args: Array[String]): Unit = {
    val log = LogManager.getLogger("myLogger")

    val spark = SparkSession
      .builder()
      .appName("AntriebsstrangStreaming")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    val toLong = udf[Long, String]( _.toLong)
    val toDatetime  = udf((time: Long) => sdf.format(new Date(time/1000000)) )

    spark.sql("set spark.sql.streaming.schemaInference=true")
    val signals = spark
      .readStream
      .parquet("hdfs://localhost:9000/data/*")


    val signalsWithFilename = signals.withColumn("filename", input_file_name)

    signalsWithFilename.createOrReplaceTempView("signals")

    val df = spark.sql("""
      SELECT substr(filename,28, 5) as filename, signal, min(time) as t_start_unix, max(time) as t_end_unix
      FROM signals
      WHERE value >= 1
      GROUP BY filename, signal
      ORDER BY filename, signal, t_start_unix, t_end_unix
    """)

    val featureDf = df
      .withColumn("t_start_datetime", toDatetime(toLong(df("t_start_unix"))))
      .withColumn("t_end_datetime", toDatetime(toLong(df("t_end_unix"))))
      .withColumn("key", df("signal"))
      .withColumn("value", concat_ws(", ", df("filename"), df("signal"), toDatetime(toLong(df("t_start_unix"))), toDatetime(toLong(df("t_end_unix"))) ))

//    Append output mode not supported when there are streaming aggregations on streaming DataFrames/DataSets without watermark
//    val query = featureDf
//      .writeStream
//      .outputMode("complete")
//      .format("console")
//      .option("path", "/Users/denniskleine/Documents/")
//      .option("checkpointLocation", "/Users/denniskleine/Documents/")
//      .option("truncate", false)
//      .start()

    val query = featureDf
      .writeStream
      .outputMode("complete")
      .format("kafka")
      .option("kafka.bootstrap.servers", "127.0.0.1:9092")
      .option("checkpointLocation", "/Users/denniskleine/Documents/")
      .option("topic", "events")
      .start()

    query.awaitTermination()
  }

}
