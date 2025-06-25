#!/usr/bin/env bash

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null 2>&1 && pwd -P)
echo "SCRIPT_DIR=${SCRIPT_DIR}"


while [ $# -gt 0 ]; do
    case "$1" in
        --class|-C)
            class="$2"
            shift
            ;;
        --deploy-mode|-D)
            deploy_mode="$2"
            shift
            ;;
        --master)
            spark_master="$2"
            shift
            ;;
        --*)
            echo "Illegal option $1"
            ;;
    esac
    shift $(( $# > 0 ? 1 : 0 ))
done


mvn(){
  "${MAVEN_HOME}"/bin/mvn $@ -s settings.xml
}


mvn clean package -Dmaven.test.skip=true






