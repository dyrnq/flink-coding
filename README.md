## flink-coding

### Build and Deploy

Use `build.sh` to compile and submit:

```bash
./build.sh -C "sample.IcebergRestCatalog"
./build.sh -C "sample.IcebergRestCatalogMultiSupport"
./build.sh -C "sample.FlinkMysqlCatalog"
./build.sh -C "sample.FlinkMysqlTable"
./build.sh -C "sample.KafkaOrderAnalysis"
```

### Build Script Options

| Option | Description | Default |
|--------|-------------|---------|
| `-C` / `--class` | Main class to submit | `sample.SparkPi` |
| `-T` / `--target` | Flink deploy target (`remote` / `local` / `yarn`) | `remote` |
| `--jar-url` | HTTP URL for shaded JAR download | `http://192.168.6.171:3000/target/flink-coding-1.0.0-shaded.jar` |

If the local shaded JAR is not available at `target/flink-coding-1.0.0-shaded.jar`, `build.sh` downloads it via `--jar-url` before submitting.

### Sample Applications

| Class | Description |
|-------|-------------|
| `IcebergRestCatalog` | Iceberg REST Catalog + SQL query (params via CLI) |
| `IcebergRestCatalogMultiSupport` | Multi-catalog Iceberg REST cross-join |
| `FlinkMysqlCatalog` | MySQL JDBC Catalog (CREATE CATALOG with SQL) |
| `FlinkMysqlTable` | MySQL JDBC Table connector |
| `KafkaOrderAnalysis` | Kafka streaming → 1-min tumbling window → alert sink |

### IcebergRestCatalog

```bash
./build.sh -C "sample.IcebergRestCatalog"
# or with custom SQL:
flink run --target remote --class sample.IcebergRestCatalog \
  flink-coding-1.0.0-shaded.jar \
  --sql "show databases from rest_catalog;"
```

### IcebergRestCatalogMultiSupport

```bash
./build.sh -C "sample.IcebergRestCatalogMultiSupport"
```

Expected output:

```text
+------------------+
|     catalog name |
+------------------+
| bar_rest_catalog |
|  default_catalog |
| foo_rest_catalog |
+------------------+
+----+-------------+-------------+-------------+
| op |          id |        data |      foo_id |
+----+-------------+-------------+-------------+
| +I |          10 | bar_data_10 |           1 |
+----+-------------+-------------+-------------+
```

### FlinkMysqlCatalog

```bash
./build.sh -C "sample.FlinkMysqlCatalog"
```

### FlinkMysqlTable

```bash
./build.sh -C "sample.FlinkMysqlTable"
```

### KafkaOrderAnalysis

```bash
./build.sh -C "sample.KafkaOrderAnalysis"
```

This is a **streaming** job — it runs indefinitely until stopped:

1. Reads JSON orders from Kafka topic `orders`
2. Computes `pending_count / total_count` ratio per 1-minute window
3. Emits alerts to Kafka topic `order-alerts` when ratio > 80%
4. Mirrors output to console via `print` connector

**Stopping & Recovery:**

```bash
# Manual stop with savepoint (allows restart from where it left off)
flink savepoint <job-id> s3://bigdata/flink-checkpoints/orders/savepoints/
flink cancel <job-id>

# Restart from savepoint
flink run -s s3://bigdata/flink-checkpoints/orders/savepoints/<savepoint-id> \
  --target remote --class sample.KafkaOrderAnalysis \
  flink-coding-1.0.0-shaded.jar
```

Auto-recovery on failure is handled by Flink's checkpointing (10s interval, EXACTLY_ONCE, `RETAIN_ON_CANCELLATION`).

### References

- [Spark counterpart](https://github.com/dyrnq/spark-scala-example)
