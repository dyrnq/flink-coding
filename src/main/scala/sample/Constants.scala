package sample

object Constants {
  // S3 / MinIO
  val s3Endpoint = "http://192.168.6.130:19000"
  val s3Region = "us-east-1"
  val s3AccessKey = "xxxXXX"
  val s3SecretKey = "xxxXXXxxxXXX"

  // Iceberg REST Catalog
  val icebergRestUri1 = "http://192.168.6.152:9001/iceberg/"
  val icebergRestUriMulti = "http://192.168.6.159:29001/iceberg"
  val icebergRestS3EndpointMulti = "http://192.168.6.159:9000"

  // Kafka
  val kafkaBootstrapServers = "192.168.6.211:9092,192.168.6.212:9092,192.168.6.213:9092"

  // MySQL
  val mysqlJdbcUrl = "jdbc:mysql://192.168.6.13:3306"
  val mysqlUsername = "root"
  val mysqlPassword = "root"

  // Checkpoint
  val checkpointPath = "s3a://bigdata/flink-checkpoints/"
}
