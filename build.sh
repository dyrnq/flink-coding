#!/usr/bin/env bash

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null 2>&1 && pwd -P)
echo "SCRIPT_DIR=${SCRIPT_DIR}"

# ── Defaults ──
class="${class:-sample.SparkPi}"
deploy_target="${deploy_target:-remote}"
jar_url="${jar_url:-http://192.168.6.171:3000/target/flink-coding-1.0.0-shaded.jar}"
shaded_jar="${shaded_jar:-target/flink-coding-1.0.0-shaded.jar}"

# ── Parse args ──
while [ $# -gt 0 ]; do
    case "$1" in
        --class|-C)
            class="$2"
            shift
            ;;
        --target|-T)
            deploy_target="$2"
            shift
            ;;
        --jar-url)
            jar_url="$2"
            shift
            ;;
        --*)
            echo "Illegal option $1"
            ;;
    esac
    shift $(( $# > 0 ? 1 : 0 ))
done

# ── Build ──
echo "Building project..."
mvn clean package -Dmaven.test.skip=true -q
echo "Build done."

# ── Resolve JAR ──
if [ -f "${shaded_jar}" ]; then
    jar_path="${shaded_jar}"
else
    echo "Local shaded JAR not found, downloading from ${jar_url}..."
    mkdir -p target
    curl -fsSL# -o "${shaded_jar}" "${jar_url}"
    jar_path="${shaded_jar}"
fi
echo "JAR path: ${jar_path}"

# ── flink run ──
case "${class}" in
    "sample.IcebergRestCatalog")
        set -x
        flink run \
            --target "${deploy_target}" \
            --class "${class}" \
            "${jar_path}" \
            --rest.uri "http://192.168.6.152:9001/iceberg/" \
            --s3.endpoint "http://192.168.6.130:19000" \
            --sql "select * from rest_catalog.testdb.my_table;"
        ;;

    "sample.IcebergRestCatalogMultiSupport")
        set -x
        flink run \
            --target "${deploy_target}" \
            --class "${class}" \
            "${jar_path}"
        ;;

    "sample.FlinkMysqlCatalog")
        set -x
        flink run \
            --target "${deploy_target}" \
            --class "${class}" \
            "${jar_path}" \
            --base-url "jdbc:mysql://192.168.6.13:3306" \
            --sql "select * from mysql_catalog.test.t_member;"
        ;;

    "sample.FlinkMysqlTable")
        set -x
        flink run \
            --target "${deploy_target}" \
            --class "${class}" \
            "${jar_path}"
        ;;

    "sample.KafkaOrderAnalysis")
        set -x
        flink run \
            --target "${deploy_target}" \
            --class "${class}" \
            "${jar_path}"
        ;;

    *)
        echo "Unknown class: ${class}"
        echo ""
        echo "Available classes:"
        echo "  sample.IcebergRestCatalog                 — Iceberg REST Catalog + SQL query"
        echo "  sample.IcebergRestCatalogMultiSupport     — Multi-catalog Iceberg REST"
        echo "  sample.FlinkMysqlCatalog                  — MySQL JDBC Catalog"
        echo "  sample.FlinkMysqlTable                    — MySQL JDBC Table"
        echo "  sample.KafkaOrderAnalysis                 — Kafka streaming → window → alert"
        exit 1
        ;;
esac

set +x
