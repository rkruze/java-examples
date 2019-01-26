package com.cockroach.example;

import java.sql.Connection;
import java.sql.SQLException;

public interface Transaction {


    void attemptTransaction(Connection conn) throws SQLException;

}
