## flink-coding

### build

```bash
./mvnw clean package -Dmaven.test.skip=true
```

### flink run

### IcebergRestCatalog

[spark ver](https://github.com/dyrnq/spark-scala-example/blob/main/src/main/scala/sample/IcebergRestSimple.scala)

```bash
## optional curl jar file from remote 
curl -o flink-coding-1.0.0.jar -fsSL# http://192.168.1.171:13000/target/flink-coding-1.0.0-shaded.jar
## run
flink \
run \
--target remote \
--class sample.IcebergRestCatalog \
flink-coding-1.0.0.jar \
--rest.uri http://192.168.1.152:9001/iceberg \
--s3.endpoint http://192.168.1.131:19000 \
--aws.region us-east-1 \
--aws.accessKeyId xxxxxx \
--aws.secretAccessKey xxxxxxxxxxxxxxxxxxxxxxxxxxxxx \
--sql "select * from rest_catalog.testdb.my_table;"
```
### FlinkMysqlCatalog

```bash
## optional curl jar file from remote 
curl -o flink-coding-1.0.0.jar -fsSL# http://192.168.1.171:13000/target/flink-coding-1.0.0-shaded.jar
## run
flink \
run \
--target remote \
--class sample.FlinkMysqlCatalog \
flink-coding-1.0.0.jar \
--catalog-name "mysql_catalog" \
--catalog-type "jdbc" \
--base-url "jdbc:mysql://192.168.6.13:3306" \
--default-database "test" \
--username "root" \
--password "root" \
--sql "select * from mysql_catalog.test.t_member;"
```