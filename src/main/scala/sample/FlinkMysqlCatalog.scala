package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object FlinkMysqlCatalog {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val defaultDatabase = params.get("default-database", "test")
    val baseUrl = params.get("base-url", Constants.mysqlJdbcUrl)
    val username = params.get("username", Constants.mysqlUsername)
    val password = params.get("password", Constants.mysqlPassword)
    val sql = params.get("sql", "")
    val catalogName = params.get("catalog-name", "mysql_catalog")
    val catalogType = params.get("catalog-type", "jdbc")

    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build()
    val tableEnv = TableEnvironment.create(settings)

    val createCatalogSql =
      s"""
         |CREATE CATALOG $catalogName WITH (
         |  'type' = '$catalogType',
         |  'default-database' = '$defaultDatabase',
         |  'base-url' = '$baseUrl',
         |  'username' = '$username',
         |  'password' = '$password'
         |);
         |""".stripMargin
    print(createCatalogSql)
    tableEnv.executeSql(createCatalogSql)
    tableEnv.executeSql(s"$sql").print()
  }

  private def setConf(): Configuration = {
    val config = new Configuration()
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
