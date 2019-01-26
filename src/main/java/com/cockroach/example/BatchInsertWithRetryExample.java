package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

public class BatchInsertWithRetryExample extends AbstractBatchInsert{

    private static final String SAVEPOINT = "COCKROACH_RESTART";


    @Override
    void commit(Connection connection, Transaction transaction) throws SQLException {

        Savepoint savepoint = connection.setSavepoint(SAVEPOINT);

        int retryCounter = 1;

        while (true) {

            boolean releaseAttempted = false;

            try {
                log.debug("attempting transaction: {}", retryCounter);
                transaction.attemptTransaction(connection);
                releaseAttempted = true;

                log.debug("attempting to release savepoint: {}", retryCounter);
                connection.releaseSavepoint(savepoint);
                log.debug("savepoint released: {}", retryCounter);
                break;
            } catch (SQLException e) {

                String sqlState = e.getSQLState();

                if (sqlState.equals("40001")) {
                    log.warn("attempting rollback: {} after error [{}]", retryCounter, e.getMessage());
                    connection.rollback(savepoint);
                    log.debug("rollback successful: {}", retryCounter);
                } else if (releaseAttempted) {
                    throw new RuntimeException("fail during release?", e);
                } else {
                    throw e;
                }
            }

            retryCounter++;
        }


        log.debug("attempting commit: {}", retryCounter);
        connection.commit();
        log.debug("commit successful: {}", retryCounter);
    }
}
