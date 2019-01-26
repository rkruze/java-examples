package com.cockroach.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertExample extends AbstractInsert {
    @Override
    void insert(int recordCount, int batchSize, Connection connection) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {

            for (int i = 0; i < recordCount; i++) {
                statement.setInt(1, RANDOM.nextInt());

                log.debug("attempting to insert record {}", i);

                statement.executeUpdate();

            }
        }
    }
}
