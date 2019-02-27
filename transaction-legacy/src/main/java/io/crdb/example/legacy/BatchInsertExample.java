package io.crdb.example.legacy;

import java.sql.Connection;
import java.sql.SQLException;

class BatchInsertExample extends AbstractBatchInsert {


    @Override
    void commit(Connection connection, TransactionWrapper transactionWrapper) throws SQLException {

        log.debug("transaction.attemptTransaction(): starting");
        transactionWrapper.attemptTransaction(connection);
        log.debug("transaction.attemptTransaction(): successful");

        log.debug("commit(): starting");
        connection.commit();
        log.debug("commit(): successful");
    }
}
