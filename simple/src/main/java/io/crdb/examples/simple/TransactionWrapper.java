package io.crdb.examples.simple;

import java.sql.Connection;
import java.sql.SQLException;

interface TransactionWrapper {

    void attemptTransaction(Connection conn) throws SQLException;

}
