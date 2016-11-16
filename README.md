A utility for running test scripts inside a cloud environment where there's a Cassandra cluster you want to talk to.

To build and run the jar:

```sh
mvn spring-boot:package
java -jar target/cassandra-drone-1.0-SNAPSHOT.jar
```

Use spring-data-cassandra configuration in an `application.yml` file to configure a connection to a Cassandra cluster:

```yaml
spring:
  data:
    cassandra:
      contact-points: localhost
```

Run scripts by POST-ing them to `http://localhost:8080/scripts/exec`, for example:

```javascript
// timer times the execution of a function
let ids = timer("Wrote 10000 rows").run(() => {
  // parallel runs a function a given number of times, distributing the work across a given number of threads
  return parallel(20).run(10000, i => {
    // uuid and timeuuid functions are provided for convenience.
    let partitionKey = uuid(),
        clusterKey = timeuuid();
        
        // The cassandra object is a Spring CassandraTemplate.
        cassandra.execute(`
          INSERT INTO test.Test (partitionKey, clusterKey, value)
          VALUES ('${partitionKey}', '${clusterKey}', '${i}')
        `);
        
         return {
           "partitionKey": partitionKey,
           "clusterKey" : clusterKey
         };
  });
});

// Anything written to the console is returned to the HTTP client
console.println(ids.get(0).partitionKey);
```

Because the Nashorn ScriptEngine doesn't provide ES6 features yet, scripts are transpiled with Babel before execution.

Script fragments can be stored by PUT-ing them to `/scripts/{script-name}`, and pulled into other scripts with `include("script-name");`.

