package io.crdb.examples.simple;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

class BatchInsertWithRetryExample extends AbstractBatchInsert {

    @Override
    void commit(Connection connection, TransactionWrapper transactionWrapper) throws SQLException {

        Savepoint savepoint = connection.setSavepoint(SAVEPOINT_NAME);

        int retryCounter = 1;

        while (true) {

            try {
                log.debug("transaction.attemptTransaction(): starting; attempt {}", retryCounter);
                transactionWrapper.attemptTransaction(connection);
                log.debug("transaction.attemptTransaction(): successful; attempt {}", retryCounter);

                log.debug("releaseSavepoint(): starting; attempt {}", retryCounter);
                connection.releaseSavepoint(savepoint);
                log.debug("releaseSavepoint(): successful; attempt {}", retryCounter);
                break;
            } catch (SQLException e) {

                String sqlState = e.getSQLState();

                log.error(String.format("error trying to commit with sql state [%s]: %s", sqlState, e.getMessage()), e);

                if (sqlState.equals("40001")) {
                    log.debug("rollback(): starting; attempt {}", retryCounter);
                    connection.rollback(savepoint);
                    log.debug("rollback(): successful; attempt {}", retryCounter);
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
