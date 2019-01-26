package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;

public class BatchInsertExample extends AbstractBatchInsert {


    @Override
    void commit(Connection connection, Transaction transaction) throws SQLException {

        log.debug("transaction.attemptTransaction(): starting");
        transaction.attemptTransaction(connection);
        log.debug("transaction.attemptTransaction(): successful");

        log.debug("connection.commit(): starting");
        connection.commit();
        log.debug("connection.commit(): successful");
    }
}
