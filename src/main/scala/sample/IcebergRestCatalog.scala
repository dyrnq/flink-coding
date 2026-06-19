package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object IcebergRestCatalog {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val awsRegion = params.get("aws.region", Constants.s3Region)
    val restUri = params.get("rest.uri", Constants.icebergRestUri1)
    val s3Endpoint = params.get("s3.endpoint", Constants.s3Endpoint)
    val awsAccessKeyId = params.get("aws.accessKeyId", Constants.s3AccessKey)
    val awsSecretAccessKey = params.get("aws.secretAccessKey", Constants.s3SecretKey)
    val sql = params.get("sql", "select * from rest_catalog.testdb.my_table;")

    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build()
    val tableEnv = TableEnvironment.create(settings)

    val createCatalogSql =
      s"""
         |CREATE CATALOG rest_catalog WITH (
         |  'type'='iceberg',
         |  'catalog-type'='rest',
         |  'uri'='$restUri',
         |  's3.endpoint'='$s3Endpoint',
         |  'client.region'='$awsRegion',
         |  's3.access-key-id'='$awsAccessKeyId',
         |  's3.secret-access-key'='$awsSecretAccessKey'
         |);
         |""".stripMargin
    print(createCatalogSql)
    tableEnv.executeSql(createCatalogSql)
    tableEnv.executeSql("SHOW catalogs;").print()
    tableEnv.executeSql(s"$sql").print()
  }

  private def setConf(): Configuration = {
    val config = new Configuration()
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
