# Observations

* retry failures always seem to happen in the first iteration after DDL statements, never in the later iterations which seems to indicated its related to timing of DDL.

With batch size of 250 and record count of 1000:

* When running BatchInsertExample in loop of 100, it fails roughly 30% of time when no pause after DDL is included.
* When running BatchInsertWithRetryExample in loop of 100, it fails roughly 35% of time when no pause after DDL is included.

* When running BatchInsertExample in loop of 100, it fails roughly 0% of time when a pause of 1000 ms after DDL is included.
* When running BatchInsertWithRetryExample in loop of 100, it fails roughly 0% of time when a pause of 1000 ms after DDL is included.
