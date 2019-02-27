package io.crdb.example.simple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class InsertExample extends AbstractInsert {
    @Override
    void insert(int recordCount, int batchSize, Connection connection) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {

            for (int i = 1; i <= recordCount; i++) {
                statement.setInt(1, RANDOM.nextInt());
                statement.executeUpdate();

                if ((i % batchSize) == 0) {
                    log.debug("processed {} records", i);
                }
            }
        }
    }
}
