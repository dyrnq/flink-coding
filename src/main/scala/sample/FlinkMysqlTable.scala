package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object FlinkMysqlTable {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)


    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build() //读设置
    val tableEnv = TableEnvironment.create(settings)
    val config = tableEnv.getConfig;



    val createCatalogSql=
      s"""
         |CREATE TABLE t_member (
         |  id                BIGINT,
         |  username          STRING,
         |  password          STRING
         |) WITH (
         |  'connector' = 'jdbc',
         |  'url' = 'jdbc:mysql://192.168.6.13:3306/test',
         |  'table-name' = 't_member',
         |  'driver' = 'com.mysql.jdbc.Driver',
         |  'username' = 'root',
         |  'password' = 'root',
         |  'lookup.cache.max-rows' = '1',
         |  'lookup.cache.ttl' = '0s'
         |);
         |""".stripMargin;
    print(createCatalogSql)
    tableEnv.executeSql(createCatalogSql)
    tableEnv.executeSql(
      """
        |SHOW tables;
            """.stripMargin).print()
    tableEnv.executeSql(
      """
        |select * from t_member;
            """.stripMargin).print()




  }

  private def setConf(): Configuration = {
    val config = new Configuration()
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
