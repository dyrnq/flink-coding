package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}
object IcebergRestCatalogMultiSupport {



  //https://gravitino.apache.org/docs/0.9.0-incubating/iceberg-rest-service/#multi-catalog-support


  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)

    val sql = params.get("sql", "select 1=1;")


    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build() //读设置
    val tableEnv = TableEnvironment.create(settings)
    val config = tableEnv.getConfig;

    config.set("key", "value")
    config.set("anotherKey", "anotherValue")


    tableEnv.executeSql(
      s"""
         |CREATE CATALOG foo_rest_catalog WITH (
         |  'type'='iceberg',
         |  'catalog-type'='rest',
         |  'uri'='http://192.168.6.159:29001/iceberg',
         |  'prefix' = 'foo',
         |  's3.endpoint'='http://192.168.6.159:9000',
         |  'client.region'='us-east-1',
         |  'client.credentials-provider'='sample.CustomCredentialProvider',
         |  'client.credentials-provider.accessKeyId'='vUR3oLMF5ds8gWCP',
         |  'client.credentials-provider.secretAccessKey'='odWFIZukYrw9dY0G5ezDKMZWbhU0S4oD'
         |);
         |""".stripMargin
    )

    tableEnv.executeSql(
      s"""
         |CREATE CATALOG bar_rest_catalog WITH (
         |  'type'='iceberg',
         |  'catalog-type'='rest',
         |  'uri'='http://192.168.6.159:29001/iceberg',
         |  'prefix' = 'bar',
         |  's3.endpoint'='http://192.168.6.159:9200',
         |  'client.region'='us-east-1',
         |  'client.credentials-provider'='sample.CustomCredentialProvider',
         |  'client.credentials-provider.accessKeyId'='vUR3oLMF5ds8gWCP',
         |  'client.credentials-provider.secretAccessKey'='odWFIZukYrw9dY0G5ezDKMZWbhU0S4oD'
         |);
         |""".stripMargin
    )


    tableEnv.executeSql(
      """
        |SHOW catalogs;
            """.stripMargin).print()

    tableEnv.executeSql(
      """
        |CREATE DATABASE IF NOT EXISTS foo_rest_catalog.foo;
            """.stripMargin).print()

    tableEnv.executeSql(
      """
        |CREATE DATABASE IF NOT EXISTS bar_rest_catalog.bar;
            """.stripMargin).print()

    tableEnv.executeSql(
      """
        |SHOW DATABASES from foo_rest_catalog;
            """.stripMargin).print()

    tableEnv.executeSql(
      """
        |SHOW DATABASES from bar_rest_catalog;
            """.stripMargin).print()

    tableEnv.executeSql("USE CATALOG foo_rest_catalog;").print();
    tableEnv.executeSql("USE foo;").print();

    tableEnv.executeSql(
      """
        |CREATE TABLE IF NOT EXISTS foo_table (
        |  id INT,
        |  data STRING
        |);
        |""".stripMargin
    ).print()

    tableEnv.executeSql(
      """
        |INSERT INTO foo_table (id, data) VALUES (1, 'data1');
        |""".stripMargin
    ).print()

    tableEnv.executeSql("USE CATALOG bar_rest_catalog;").print();
    tableEnv.executeSql("USE bar;").print();
    tableEnv.executeSql(
      """
        |CREATE TABLE IF NOT EXISTS bar_table (
        |  id INT, data STRING,foo_id INT
        |);
        |""".stripMargin
    ).print()

    tableEnv.executeSql(
      """
        |INSERT INTO bar_table (id, data, foo_id) VALUES (10, 'bar_data_10',1);
        |""".stripMargin
    ).print()

    tableEnv.executeSql(
      """
        |SELECT * FROM
        |bar_rest_catalog.bar.bar_table as bar,
        |foo_rest_catalog.foo.foo_table as foo
        |WHERE bar.foo_id=foo.id;
        |""".stripMargin
    ).print()


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
