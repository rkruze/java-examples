package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;

public interface TransactionWrapper {

    void attemptTransaction(Connection conn) throws SQLException;

}
