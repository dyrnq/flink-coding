package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object FlinkMysqlCatalog {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val default_database = params.get("default-database", "test")
    val base_url = params.get("base-url", "")
    val username = params.get("username", "")
    val password = params.get("password", "")
    val sql = params.get("sql", "")
    val catalog_name = params.get("catalog-name", "mysql_catalog")
    val catalog_type = params.get("catalog-type", "jdbc")
    val settings = EnvironmentSettings.newInstance().inStreamingMode()
      .withConfiguration(setConf())
      .build() //读设置
    val tableEnv = TableEnvironment.create(settings)
    val config = tableEnv.getConfig;



    val createCatalogSql=
      s"""
         |CREATE CATALOG $catalog_name WITH (
         |  'type' = '$catalog_type',
         |  'default-database' = '$default_database',
         |  'base-url' = '$base_url',
         |  'username' = '$username',
         |  'password' = '$password'
         |);
         |""".stripMargin;
    print(createCatalogSql)
    tableEnv.executeSql(createCatalogSql)

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
