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

### IcebergRestCatalogMultiSupport


```bash
flink \
run \
--target remote \
--class sample.IcebergRestCatalogMultiSupport \
flink-coding-1.0.0.jar
```


```bash

+------------------+
|     catalog name |
+------------------+
| bar_rest_catalog |
|  default_catalog |
| foo_rest_catalog |
+------------------+
3 rows in set
OK
OK
+---------------+
| database name |
+---------------+
|           foo |
+---------------+
1 row in set
+---------------+
| database name |
+---------------+
|           bar |
+---------------+
1 row in set
OK
OK
OK
Job has been submitted with JobID 03d775bf35e98c5efcd82197e6169b44
-1
OK
OK
OK
Job has been submitted with JobID 3b18b4f8269c8dda7524a662c44399dc
-1
Job has been submitted with JobID 99d4b350ad1466e03309c2f6ecf9fc8d
+----+-------------+--------------------------------+-------------+-------------+--------------------------------+
| op |          id |                           data |      foo_id |         id0 |                          data0 |
+----+-------------+--------------------------------+-------------+-------------+--------------------------------+
| +I |          10 |                    bar_data_10 |           1 |           1 |                          data1 |
+----+-------------+--------------------------------+-------------+-------------+--------------------------------+
3 rows in set
Job has been submitted with JobID 275395d181a3368bea013543282a76e6
+----+--------+
| op | EXPR$0 |
+----+--------+
| +I |   TRUE |
+----+--------+
1 row in set


```