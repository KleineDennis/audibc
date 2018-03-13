import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import org.apache.log4j.LogManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Antriebsstrang {

  def main(args: Array[String]): Unit = {
    val log = LogManager.getLogger("myLogger")

    val spark = SparkSession
      .builder()
      .appName("Antriebsstrang")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val filename = "file"
    val counter = 1

    val signals = spark.read.parquet("hdfs://localhost:9000/data/" + filename + counter)

    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    val toLong = udf[Long, String]( _.toLong)
    val toDatetime  = udf((time: Long) => sdf.format(new Date(time/1000000)) )

//    val df = signals
//      .filter("value >= 1")
//      .groupBy("signal")
//      .agg(min("time"), max("time"))
//
//    val df2 = df
//      .withColumn("t_start_datetime", toDatetime(toLong(df("t_start_unix"))))
//      .withColumn("t_end_datetime", toDatetime(toLong(df("t_end_unix"))))
//      .withColumn("filename", col(filename+counter))
//
//    df2.show()

    signals.createOrReplaceTempView("signals")

    val df = spark.sql("""
      SELECT signal, min(time) as t_start_unix, max(time) as t_end_unix
      FROM signals
      WHERE value >= 1
      GROUP BY signal
      ORDER BY signal
    """)

    val featureDf = df
      .withColumn("t_start_datetime", toDatetime(toLong(df("t_start_unix"))))
      .withColumn("t_end_datetime", toDatetime(toLong(df("t_end_unix"))))
      .withColumn("filename", lit(filename + counter))

    featureDf.show()

    featureDf.collect.foreach(x => log.info(x.mkString(", ")))

    featureDf
      .map(x => x.mkString("|"))
      .repartition(1)
      .write
      .mode("overwrite")
      .text("/Users/denniskleine/Documents/" + filename + counter)

  }

}


