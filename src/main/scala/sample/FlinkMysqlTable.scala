package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object FlinkMysqlTable {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val jdbcUrl = params.get("url", Constants.mysqlJdbcUrl + "/test")
    val username = params.get("username", Constants.mysqlUsername)
    val password = params.get("password", Constants.mysqlPassword)

    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build()
    val tableEnv = TableEnvironment.create(settings)

    val createCatalogSql =
      s"""
         |CREATE TABLE t_member (
         |  id                BIGINT,
         |  username          STRING,
         |  password          STRING
         |) WITH (
         |  'connector' = 'jdbc',
         |  'url' = '$jdbcUrl',
         |  'table-name' = 't_member',
         |  'driver' = 'com.mysql.jdbc.Driver',
         |  'username' = '$username',
         |  'password' = '$password',
         |  'lookup.cache.max-rows' = '1',
         |  'lookup.cache.ttl' = '0s'
         |);
         |""".stripMargin
    print(createCatalogSql)
    tableEnv.executeSql(createCatalogSql)
    tableEnv.executeSql("SHOW tables;").print()
    tableEnv.executeSql("select * from t_member;").print()
  }

  private def setConf(): Configuration = {
    val config = new Configuration()
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
