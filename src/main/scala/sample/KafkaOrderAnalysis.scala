package sample

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

/**
 * Flink Streaming Example — Kafka source → window aggregation → alert sink
 *
 * Equivalent to Spark project: OrdersAnalysisKafka.scala
 *
 * Flow:
 *   1. Define Kafka source table (orders topic)
 *   2. 1-minute tumbling window aggregation
 *   3. Count pending ratio per window
 *   4. Emit alerts to Kafka sink + print sink
 *
 * Recovery:
 *   - Auto-restore from checkpoint on failure (10s interval, EXACTLY_ONCE)
 *   - Manual stop: flink savepoint <job-id> → flink run -s <path>
 *   - Checkpoint path: s3a://bigdata/flink-checkpoints/orders
 */
object KafkaOrderAnalysis {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val kafkaServers = params.get("kafka.servers", Constants.kafkaBootstrapServers)
    val checkpointPath = params.get("checkpoint.path", Constants.checkpointPath + "orders")

    val settings = EnvironmentSettings.newInstance()
      .inStreamingMode()
      .withConfiguration(setConf(checkpointPath))
      .build()
    val tableEnv = TableEnvironment.create(settings)

    // ── 1. Kafka Source Table ──
    tableEnv.executeSql(
      s"""
         |CREATE TABLE orders_source (
         |  id          BIGINT,
         |  product     STRING,
         |  amount      DECIMAL(10, 2),
         |  status      STRING,
         |  `timestamp` TIMESTAMP_LTZ(3),
         |  WATERMARK FOR `timestamp` AS `timestamp` - INTERVAL '5' SECOND
         |) WITH (
         |  'connector' = 'kafka',
         |  'topic' = 'orders',
         |  'properties.bootstrap.servers' = '$kafkaServers',
         |  'properties.group.id' = 'flink-order-analysis',
         |  'scan.startup.mode' = 'latest-offset',
         |  'format' = 'json',
         |  'json.fail-on-missing-field' = 'false',
         |  'json.ignore-parse-errors' = 'true'
         |)
         |""".stripMargin
    )

    // ── 2. Alert Sink Table (Kafka) ──
    tableEnv.executeSql(
      s"""
         |CREATE TABLE alerts_sink (
         |  window_start   TIMESTAMP_LTZ(3),
         |  window_end     TIMESTAMP_LTZ(3),
         |  pending_count  BIGINT,
         |  total_count    BIGINT,
         |  pending_ratio  DOUBLE,
         |  alert_msg      STRING
         |) WITH (
         |  'connector' = 'kafka',
         |  'topic' = 'order-alerts',
         |  'properties.bootstrap.servers' = '$kafkaServers',
         |  'format' = 'json'
         |)
         |""".stripMargin
    )

    // ── 3. Print Sink (console debugging) ──
    tableEnv.executeSql(
      """
        |CREATE TABLE print_sink (
        |  window_start   TIMESTAMP_LTZ(3),
        |  window_end     TIMESTAMP_LTZ(3),
        |  pending_count  BIGINT,
        |  total_count    BIGINT,
        |  pending_ratio  DOUBLE,
        |  alert_msg      STRING
        |) WITH (
        |  'connector' = 'print'
        |)
        |""".stripMargin
    )

    // ── 4. Windowed aggregation view ──
    tableEnv.executeSql(
      """
        |CREATE TEMPORARY VIEW windowed_orders AS
        |SELECT
        |  window_start,
        |  window_end,
        |  COUNT(CASE WHEN status = 'pending' THEN 1 END) AS pending_count,
        |  COUNT(*)                                          AS total_count
        |FROM TABLE(
        |  TUMBLE(TABLE orders_source, DESCRIPTOR(`timestamp`), INTERVAL '1' MINUTE)
        |)
        |GROUP BY window_start, window_end
        |""".stripMargin
    )

    // ── 5. Compute ratio + emit to both sinks ──
    val alertSql =
      """
        |INSERT INTO %s
        |SELECT
        |  window_start,
        |  window_end,
        |  pending_count,
        |  total_count,
        |  CAST(pending_count AS DOUBLE) / CAST(total_count AS DOUBLE) AS pending_ratio,
        |  CASE
        |    WHEN CAST(pending_count AS DOUBLE) / CAST(total_count AS DOUBLE) > 0.8
        |    THEN CONCAT('ALERT: high pending ratio ',
        |         CAST(CAST(pending_count AS DOUBLE) / CAST(total_count AS DOUBLE) AS STRING))
        |    ELSE 'OK'
        |  END AS alert_msg
        |FROM windowed_orders
        |""".stripMargin

    tableEnv.executeSql(alertSql.format("alerts_sink"))
    tableEnv.executeSql(alertSql.format("print_sink")).await()
  }

  private def setConf(checkpointPath: String): Configuration = {
    val config = new Configuration()
    // Checkpoint — essential for failure recovery
    config.setString("execution.checkpointing.interval", "10000")
    config.setString("execution.checkpointing.mode", "EXACTLY_ONCE")
    config.setString("execution.checkpointing.externalized-checkpoint-retention",
      "RETAIN_ON_CANCELLATION")
    config.setString("state.checkpoints.dir", checkpointPath)
    // Savepoint path for manual stop → restart
    config.setString("state.savepoints.dir", checkpointPath + "/savepoints")
    config.setString("sql-client.execution.result-mode", "tableau")
    config
  }
}
