#!/usr/bin/env bash

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null 2>&1 && pwd -P)
echo "SCRIPT_DIR=${SCRIPT_DIR}"

# ── Defaults ──
class="${class:-sample.FlinkPi}"
deploy_target="${deploy_target:-remote}"
flink_image="${flink_image:-flink:1.20.5-scala_2.12-java17}"
shaded_jar="${shaded_jar:-target/flink-coding-1.0.0-shaded.jar}"
jar_url="${jar_url:-http://192.168.6.171:3000/target/flink-coding-1.0.0-shaded.jar}"
flink_home="/opt/flink"

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
        --image)
            flink_image="$2"
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

# ── Resolve shaded JAR ──
if [ -f "${shaded_jar}" ]; then
    jar_name=$(basename "${shaded_jar}")
else
    echo "Local shaded JAR not found, downloading from ${jar_url}..."
    mkdir -p target
    curl -fsSL# -o "${shaded_jar}" "${jar_url}"
    jar_name=$(basename "${shaded_jar}")
fi
echo "JAR: ${shaded_jar}"

# ── Class-specific extra args (passed to main method) ──
extra_args=""
case "${class}" in
    "sample.FlinkPi")
        extra_args="--slices 20"
        ;;
    "sample.IcebergRestCatalog")
        extra_args="--rest.uri http://192.168.6.152:9001/iceberg/ --s3.endpoint http://192.168.6.130:19000 --sql \"select * from rest_catalog.testdb.my_table;\""
        ;;
    "sample.FlinkMysqlCatalog")
        extra_args="--base-url jdbc:mysql://192.168.6.13:3306 --sql \"select * from mysql_catalog.test.t_member;\""
        ;;
esac

# ── Docker run flink ──
set -x
docker run \
  -it \
  --rm \
  --network=host \
  -v "${SCRIPT_DIR}/target":/target \
  -v "${SCRIPT_DIR}/conf/flink-conf.yaml":"${flink_home}/conf/flink-conf.yaml" \
  "${flink_image}" \
  ${flink_home}/bin/flink run \
    --target "${deploy_target}" \
    --class "${class}" \
    "/target/${jar_name}" \
    ${extra_args}
set +x
