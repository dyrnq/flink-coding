## flink-coding

This project demonstrates Flink integrations with Iceberg, Kafka, and MySQL
using S3 (MinIO) as the storage backend.

### Cluster Environment

| Component | Version | Notes |
|-----------|---------|-------|
| Flink | 1.20.5 | `flink:1.20.5-scala_2.12-java17` Docker image |
| Scala | 2.12.21 | |
| Java | 17 | From Docker image |
| Apache Iceberg | 1.11.0 | REST Catalog + S3FileIO |
| Kafka | 3.8.1 | |
| MySQL | 8.0 | JDBC Catalog connector |

### Infrastructure Nodes

| Node | Address | Role |
|------|---------|------|
| flink-jobmanager | `192.168.6.x:8081` | Flink JobManager (REST API) |
| minio | `192.168.6.130:19000` | S3-compatible storage |
| kafka | `192.168.6.211:9092, 192.168.6.212:9092, 192.168.6.213:9092` | Kafka bootstrap servers |
| iceberg-rest | `192.168.6.152:9001` | Iceberg REST Catalog |
| iceberg-rest-multi | `192.168.6.159:29001` | Iceberg REST Catalog (multi-tenant) |
| mysql | `192.168.6.13:3306` | MySQL |
| http-jar-server | `192.168.6.171:3000` | HTTP server for JAR distribution |

### Deployment Architecture

```
                               ┌─────────────────────────────────┐
                               │    MinIO (S3)                   │
                               │    192.168.6.130:19000          │
                               └──────────────┬──────────────────┘
                                              │
                    ┌─────────────────────────┼──────────────────────────┐
                    │                         │                          │
         ┌──────────┴──────────┐   ┌──────────┴──────────┐   ┌──────────┴──────────┐
         │ Iceberg REST Catalog │   │ Flink Cluster        │   │ MySQL               │
         │ 192.168.6.152:9001   │   │ 1.20.5 (Docker)     │   │ 192.168.6.13:3306   │
         │ 192.168.6.159:29001  │   │                      │   │                     │
         └──────────────────────┘   └──────────┬───────────┘   └─────────────────────┘
                                               │
                              ┌────────────────┼────────────────┐
                              │                │                │
                    ┌─────────┴─────┐  ┌───────┴──────┐  ┌──────┴──────┐
                    │ Kafka Cluster │  │ HTTP Server  │  │ Checkpoint  │
                    │ 3 node        │  │ :3000 (JAR)  │  │ S3          │
                    └───────────────┘  └──────────────┘  └─────────────┘
```

### Build and Deploy

Use `build.sh` to compile and submit via Docker:

```bash
./build.sh -C "sample.FlinkPi"
./build.sh -C "sample.IcebergRestCatalog"
./build.sh -C "sample.IcebergRestCatalogMultiSupport"
./build.sh -C "sample.FlinkMysqlCatalog"
./build.sh -C "sample.FlinkMysqlTable"
./build.sh -C "sample.KafkaOrderAnalysis"
```

`build.sh` builds the shaded uber-JAR locally, then runs `flink run`
inside the official `flink:1.20.5-scala_2.12-java17` Docker container,
mounting `conf/flink-conf.yaml` for S3 and checkpoint config.

### Build Script Options

| Option | Description | Default |
|--------|-------------|---------|
| `-C` / `--class` | Main class to submit | `sample.KafkaOrderAnalysis` |
| `-T` / `--target` | Flink deploy target (`remote` / `local` / `yarn`) | `remote` |
| `--image` | Flink Docker image | `flink:1.20.5-scala_2.12-java17` |
| `--jar-url` | HTTP fallback URL for shaded JAR | `http://192.168.6.171:3000/target/...` |

### conf/flink-conf.yaml

Preconfigured with:
- S3/MinIO endpoint + path-style access
- Checkpoint interval (10s, EXACTLY_ONCE)
- Checkpoint/savepoint paths: `s3://bigdata/flink-checkpoints/`
- `classloader.resolve-order: parent-first` (avoids shaded JAR conflicts)

### Sample Applications

| Class | Description |
|-------|-------------|
| `FlinkPi` | Monte Carlo Pi estimation using DataStream API |
| `IcebergRestCatalog` | Iceberg REST Catalog + SQL query (params via CLI) |
| `IcebergRestCatalogMultiSupport` | Multi-catalog Iceberg REST cross-join |
| `FlinkMysqlCatalog` | MySQL JDBC Catalog (CREATE CATALOG with SQL) |
| `FlinkMysqlTable` | MySQL JDBC Table connector |
| `KafkaOrderAnalysis` | Kafka streaming → 1-min tumbling window → alert sink |

### KafkaOrderAnalysis — Streaming Job Lifecycle

```bash
# Submit (runs indefinitely)
./build.sh -C "sample.KafkaOrderAnalysis"
```

**Stopping & Recovery:**

```bash
# Manual stop with savepoint
flink savepoint <job-id> s3://bigdata/flink-checkpoints/orders/savepoints/
flink cancel <job-id>

# Restart from savepoint
./build.sh -C "sample.KafkaOrderAnalysis"
# (if needed, add -s <savepoint-path> to the flink run command in build.sh)
```

Auto-recovery is handled by Flink checkpointing (10s interval, EXACTLY_ONCE,
`RETAIN_ON_CANCELLATION`). The configuration is in `conf/flink-conf.yaml`.

### References

- [Spark counterpart](https://github.com/dyrnq/spark-scala-example)
- [Flink Checkpoints](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/ops/state/checkpoints/)
- [Flink Savepoints](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/ops/state/savepoints/)
