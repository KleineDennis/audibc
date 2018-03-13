

val string = "hdfs://localhost:9000/data/file5/part-00000-9872d8c3-f6eb-4147-bb2a-32b78c028d57.snappy.parquet"

val length = string.length-63
val res = string.substring(27, string.length-63)
