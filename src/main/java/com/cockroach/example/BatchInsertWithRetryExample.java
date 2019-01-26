package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

class BatchInsertWithRetryExample extends AbstractBatchInsert {

    @Override
    void commit(Connection connection, TransactionWrapper transactionWrapper) throws SQLException {

        Savepoint savepoint = connection.setSavepoint(SAVEPOINT_NAME);

        int retryCounter = 1;

        while (true) {

            boolean releaseAttempted = false;

            try {
                log.debug("transaction.attemptTransaction(): starting; attempt {}", retryCounter);
                transactionWrapper.attemptTransaction(connection);
                log.debug("transaction.attemptTransaction(): successful; attempt {}", retryCounter);
                releaseAttempted = true;

                log.debug("releaseSavepoint(): starting; attempt {}", retryCounter);
                connection.releaseSavepoint(savepoint);
                log.debug("releaseSavepoint(): successful; attempt {}", retryCounter);
                break;
            } catch (SQLException e) {

                String sqlState = e.getSQLState();

                log.error(e.getMessage(), e);

                if (sqlState.equals("40001")) {
                    log.debug("rollback(): starting; attempt {}", retryCounter);
                    connection.rollback(savepoint);
                    log.debug("rollback(): successful; attempt {}", retryCounter);
                } else if (releaseAttempted) {
                    throw new RuntimeException("fail during release?", e);
                } else {
                    throw e;
                }
            }

            retryCounter++;
        }


        log.debug("commit(): starting; attempt {}", retryCounter);
        connection.commit();
        log.debug("commit(): successful; attempt {}", retryCounter);
    }
}
