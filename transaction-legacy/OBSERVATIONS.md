# Observations

* when transactions are enabled with inserts on a three node cluster, failures occur roughly `30%` of the time
* when transactions are enabled with inserts on a five node cluster, failures occur roughly `50%` of the time
* when transactions are __enabled__ with inserts on a one node cluster, everything works as expected; __zero failures__
* when transactions are __enabled__ with inserts on a three node cluster and a `500ms` pause before first `insert` everything works as expected; __zero failures__
* when transactions are __disabled__ with inserts on a three node cluster, everything works as expected; __zero failures__
* failures always seem to happen in the first iteration after DDL statements, never in the later iterations which seems to indicated its related to timing of DDL.
* the duration of the DDL statements, as captured in Java, does not seem to influence the failure rate. In other words, longer DDL execution times do not equal higher failure rates; shorter executions times do not reduce failure rates.  This suggests the conflict happens because of something happening in the cluster after JDBC believes the DDL statement has completed.
* retry works but batched statements are lost... bug?
* presence or type of loadbalancer does not impact failures significantly

With batch size of `250` and record count of `1000`:

## Three Node Cluster

### No Pause
* When running `BatchInsertExample` in loop of `100`, it fails roughly `30%` of time when no pause after DDL is included.
* When running `BatchInsertWithRetryExample` in loop of `100`, it fails roughly `35%` of time when no pause after DDL is included.

### No Pause, No Transaction
* When running `BatchInsertExample` in loop of `100` with `transactions` disabled, it fails roughly `0%` of time when no pause after DDL is included.

### No Pause, RETURNING NOTHING
* When running `BatchInsertExample` in loop of `100` with `RETURNING NOTHING` appended to the insert statement, it fails roughly `64%` of time when no pause after DDL is included.

### With Pause (1000ms)
* When running `BatchInsertExample` in loop of `100`, it fails roughly `0%` of time when a pause of `1000ms` after DDL is included.
* When running `BatchInsertWithRetryExample` in loop of `100`, it fails roughly `0%` of time when a pause of `1000ms` after DDL is included.

### With Pause (500ms)
* When running `BatchInsertExample` in loop of `100`, it fails roughly `0%` of time when a pause of `500ms` after DDL is included.

### With Pause (300ms)
* When running `BatchInsertExample` in loop of `100`, it fails roughly `.04%` of time when a pause of `300ms` after DDL is included.

### With Pause (200ms)
* When running `BatchInsertExample` in loop of `100`, it fails roughly `18%` of time when a pause of `200ms` after DDL is included.

### With Pause (100ms)
* When running `BatchInsertExample` in loop of `100`, it fails roughly `48%` of time when a pause of `100ms` after DDL is included.

### With Pause (10ms)
* When running `BatchInsertExample` in loop of `100`, it fails roughly `35%` of time when a pause of `10ms` after DDL is included.

## Three Node Cluster without Load Balancer

### No Pause
* When running `BatchInsertExample` in loop of `100`, it fails roughly `67%` of time when no pause after DDL is included.

## Single Node Cluster

### No Pause
* When running `BatchInsertExample` in loop of `100`, it fails roughly `0%` of time when no pause after DDL is included.

## Five Node Cluster

### No Pause
* When running `BatchInsertExample` in loop of `100`, it fails roughly `47%` of time when no pause after DDL is included.


# Questions
* could this be related to the use of a load balancer? (doesn't seem to be the case)
* surprised by this statement "...do not include the INSERT statements within a transaction." here: https://www.cockroachlabs.com/docs/v2.1/performance-best-practices-overview.html#use-multi-row-insert-statements-for-bulk-inserts-into-existing-tables
    * __when disabling transactions, i don't see any issues__
* `INSERT` docs suggest that `RETURN NOTHING` can be used for inserts inside a transaction... "Within a transaction, use RETURNING NOTHING to return nothing in the response, not even the number of rows affected." This led to higher failure rates in my tests.
* when batched statements are lost during retry is this an application bug or a db bug?
* is the placement of `connection.setAutoCommit(false);` problematic?  should it be right before commit attempt?
* does using a connection per statement change behavior?

# Execute Batch Issue?

When implementing retry logic the following is observed.  Need to figure out how to save/replay statements added to batch.

- Step 1: calls `executeBatch` and succeeds; internally `executeBatch` clears batch statements
- Step 2: call to `releaseSavepoint` fails with `RETRY_ASYNC_WRITE_FAILURE`, triggering rollback
- Step 3: `rollback` attempted and succeeds
- Step 4: second call to `executeBatch` succeeds, but original statements cleared in Step 1
- Step 5: second call to `releaseSavepoint` succeeds
- Step 6: `commit` succeeds and process continues but statements from original execution are lost and never saved which is bad.

Retry logic...
```java
Savepoint savepoint = connection.setSavepoint(SAVEPOINT_NAME);

int retryCounter = 1;

while (true) {

    try {
        // Step 1 & Step 4
        transactionWrapper.attemptTransaction(connection);

        // Step 2 & Step 5
        connection.releaseSavepoint(savepoint);
        break;
    } catch (SQLException e) {

        String sqlState = e.getSQLState();

        if (sqlState.equals("40001")) {
            // Step 3
            connection.rollback(savepoint);
        } else {
            throw e;
        }
    }

    retryCounter++;
}


// Step 6
connection.commit();
```
