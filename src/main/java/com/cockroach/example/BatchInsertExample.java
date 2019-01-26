package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;

public class BatchInsertExample extends AbstractBatchInsert {


    @Override
    void commit(Connection connection, Transaction transaction) throws SQLException {

        log.debug("attempting transaction...");
        transaction.attemptTransaction(connection);

        log.debug("attempting commit...");
        connection.commit();
    }
}
