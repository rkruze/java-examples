# Observations

* retry failures always seem to happen in the first iteration after DDL statements, never in the later iterations which seems to indicated its related to timing of DDL.
* the duration of the DDL statements as captured in Java does not seem to influence the failure rate. In other words, longer DDL statements do not equal higher failure rates; shorter executions do not reduce failure rates.  This suggests the conflict happens because of something happening in the cluster and after JDBC believes the DDL statement has completed.

With batch size of `250` and record count of `1000`:

## No Pause
* When running BatchInsertExample in loop of `100`, it fails roughly `30%` of time when no pause after DDL is included.
* When running BatchInsertWithRetryExample in loop of `100`, it fails roughly `35%` of time when no pause after DDL is included.

## With Pause (1000ms)
* When running BatchInsertExample in loop of `100`, it fails roughly `0%` of time when a pause of `1000ms` after DDL is included.
* When running BatchInsertWithRetryExample in loop of `100`, it fails roughly `0%` of time when a pause of `1000ms` after DDL is included.

## With Pause (500ms)
* When running BatchInsertExample in loop of `100`, it fails roughly `0%` of time when a pause of `500ms` after DDL is included.

## With Pause (300ms)
* When running BatchInsertExample in loop of `100`, it fails roughly `.04%` of time when a pause of `300ms` after DDL is included.

## With Pause (200ms)
* When running BatchInsertExample in loop of `100`, it fails roughly `18%` of time when a pause of `200ms` after DDL is included.

## With Pause (100ms)
* When running BatchInsertExample in loop of `100`, it fails roughly `48%` of time when a pause of `100ms` after DDL is included.

## With Pause (10ms)
* When running BatchInsertExample in loop of `100`, it fails roughly `35%` of time when a pause of `10ms` after DDL is included.


# Questions
* could this be related to the use of a load balancer?