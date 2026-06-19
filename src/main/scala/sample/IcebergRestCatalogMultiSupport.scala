package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object IcebergRestCatalogMultiSupport {

  // https://gravitino.apache.org/docs/0.9.0-incubating/iceberg-rest-service/#multi-catalog-support

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val sql = params.get("sql", "select 1=1;")

    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build()
    val tableEnv = TableEnvironment.create(settings)

    tableEnv.executeSql(
      s"""
         |CREATE CATALOG foo_rest_catalog WITH (
         |  'type'='iceberg',
         |  'catalog-type'='rest',
         |  'uri'='${Constants.icebergRestUriMulti}',
         |  'prefix' = 'foo',
         |  's3.endpoint'='${Constants.icebergRestS3EndpointMulti}',
         |  'client.region'='${Constants.s3Region}',
         |  's3.access-key-id'='${Constants.s3AccessKey}',
         |  's3.secret-access-key'='${Constants.s3SecretKey}'
         |);
         |""".stripMargin
    )

    tableEnv.executeSql(
      s"""
         |CREATE CATALOG bar_rest_catalog WITH (
         |  'type'='iceberg',
         |  'catalog-type'='rest',
         |  'uri'='${Constants.icebergRestUriMulti}',
         |  'prefix' = 'bar',
         |  's3.endpoint'='http://192.168.6.159:9200',
         |  'client.region'='${Constants.s3Region}',
         |  's3.access-key-id'='${Constants.s3AccessKey}',
         |  's3.secret-access-key'='${Constants.s3SecretKey}'
         |);
         |""".stripMargin
    )

    tableEnv.executeSql("SHOW catalogs;").print()
    tableEnv.executeSql("CREATE DATABASE IF NOT EXISTS foo_rest_catalog.foo;").print()
    tableEnv.executeSql("CREATE DATABASE IF NOT EXISTS bar_rest_catalog.bar;").print()
    tableEnv.executeSql("SHOW DATABASES from foo_rest_catalog;").print()
    tableEnv.executeSql("SHOW DATABASES from bar_rest_catalog;").print()

    tableEnv.executeSql("USE CATALOG foo_rest_catalog;").print()
    tableEnv.executeSql("USE foo;").print()
    tableEnv.executeSql(
      """
        |CREATE TABLE IF NOT EXISTS foo_table (
        |  id INT NOT NULL PRIMARY KEY NOT ENFORCED,
        |  data STRING
        |);
        |""".stripMargin
    ).print()
    tableEnv.executeSql("INSERT INTO foo_table (id, data) VALUES (1, 'data1');").print()

    tableEnv.executeSql("USE CATALOG bar_rest_catalog;").print()
    tableEnv.executeSql("USE bar;").print()
    tableEnv.executeSql(
      """
        |CREATE TABLE IF NOT EXISTS bar_table (
        |  id INT NOT NULL PRIMARY KEY NOT ENFORCED, data STRING, foo_id INT
        |);
        |""".stripMargin
    ).print()
    tableEnv.executeSql("INSERT INTO bar_table (id, data, foo_id) VALUES (10, 'bar_data_10',1);").print()

    tableEnv.executeSql(
      """
        |SELECT * FROM
        |bar_rest_catalog.bar.bar_table as bar,
        |foo_rest_catalog.foo.foo_table as foo
        |WHERE bar.foo_id=foo.id;
        |""".stripMargin
    ).print()

    tableEnv.executeSql(s"$sql").print()
  }

  private def setConf(): Configuration = {
    val config = new Configuration()
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
