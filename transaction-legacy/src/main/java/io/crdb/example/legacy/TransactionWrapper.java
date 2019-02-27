package io.crdb.example.legacy;

import java.sql.Connection;
import java.sql.SQLException;

interface TransactionWrapper {

    void attemptTransaction(Connection conn) throws SQLException;

}
