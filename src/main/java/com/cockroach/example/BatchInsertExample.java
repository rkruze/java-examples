package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;

public class BatchInsertExample extends AbstractBatchInsert {


    @Override
    void commit(Connection connection, TransactionWrapper transactionWrapper) throws SQLException {

        log.debug("transaction.attemptTransaction(): starting");
        transactionWrapper.attemptTransaction(connection);
        log.debug("transaction.attemptTransaction(): successful");

        log.debug("connection.commit(): starting");
        connection.commit();
        log.debug("connection.commit(): successful");
    }
}
