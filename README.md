Start simple cluster in Docker with `docker` > `./run.sh`

Configure with `cockroach.properties`

To build...
```
> mvn clean package
```

To run...
```
# Batch Insert Test
> java -jar java-examples-1.0-SNAPSHOT.jar bi

# Batch Insert Test with Retry
> java -jar java-examples-1.0-SNAPSHOT.jar bir
```

