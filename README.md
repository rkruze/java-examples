# Java Examples
Collection of Java Examples for CockroachDB

## Getting Started

1) Start simple cluster in Docker with `docker` > `./run.sh`.  For more info see [README](docker/README.md).
2) Configure parameters with `cockroach.properties`.  For more info see [here](src/main/resources/cockroach.properties).
3) Build `java-examples-1.0-SNAPSHOT.jar` with Maven.  
```bash
./mvnw clean package
```
4) Run test
```bash
# Insert Test
java -jar java-examples-1.0-SNAPSHOT.jar i

# Batch Insert Test
java -jar java-examples-1.0-SNAPSHOT.jar bi

# Batch Insert Test with Retry
java -jar java-examples-1.0-SNAPSHOT.jar bir
```

## Miscellaneous
You can view Postgres Driver logging by adding the following parameter at startup:
```bash
-Djava.util.logging.config.file=src/main/resources/logging.properties
```

