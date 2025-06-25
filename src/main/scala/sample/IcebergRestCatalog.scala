package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object IcebergRestCatalog {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val awsRegion = params.get("aws.region", "us-east-1")
    val restUri = params.get("rest.uri", "")
    val s3Endpoint = params.get("s3.endpoint", "")
    val awsAccessKeyId = params.get("aws.accessKeyId", "")
    val awsSecretAccessKey = params.get("aws.secretAccessKey", "")
    val sql = params.get("sql", "")
//    val awsRegion = System.getProperty("aws.region", "us-east-1")
//    val awsAccessKeyId = System.getProperty("aws.accessKeyId", "default_value")
//    val awsSecretAccessKey = System.getProperty("aws.secretAccessKey", "default_value")
//    val s3Endpoint = System.getProperty("s3.endpoint", "default_value")
//    val restUri = System.getProperty("rest.uri", "default_value")


    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build() //读设置
    val tableEnv = TableEnvironment.create(settings)
    val config = tableEnv.getConfig;

    config.set("key", "value")
    config.set("anotherKey", "anotherValue")

    val createCatalogSql=
      s"""
         |CREATE CATALOG rest_catalog WITH (
         |  'type'='iceberg',
         |  'catalog-type'='rest',
         |  'uri'='$restUri',
         |  's3.endpoint'='$s3Endpoint',
         |  'client.region'='$awsRegion',
         |  'client.credentials-provider'='sample.CustomCredentialProvider',
         |  'client.credentials-provider.accessKeyId'='$awsAccessKeyId',
         |  'client.credentials-provider.secretAccessKey'='$awsSecretAccessKey'
         |);
         |""".stripMargin;
    print(createCatalogSql)
    tableEnv.executeSql(createCatalogSql)
    tableEnv.executeSql(
      """
        |SHOW catalogs;
            """.stripMargin).print()

//    tableEnv.executeSql(
//      """
//        |SHOW catalogs;
//            """.stripMargin).print()

    tableEnv.executeSql(
      s"""
        |$sql
            """.stripMargin).print()


  }

  private def setConf(): Configuration = {
    val config = new Configuration()
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
