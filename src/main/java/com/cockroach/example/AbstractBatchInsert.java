package com.cockroach.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;

public abstract class AbstractBatchInsert  {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS accounts";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT)";
    private static final String INSERT = "INSERT INTO accounts(balance) VALUES(?)";



    private void executeUpdate(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void printCounts(int[] counts) {
        log.debug("batch statement results size = " + counts.length);

        for (int count : counts) {
            if (count == -2) {
                log.trace("batch SUCCESS_NO_INFO");
            } else if (count == -3) {
                log.trace("batch EXECUTE_FAILED");
            } else {
                log.trace("batch update count = " + count);
            }
        }
    }



    private void verifyCount(int recordCount, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select count(*) from accounts")) {

            int count = 0;

            if (rs.next()) {
                count = rs.getInt(1);
            }

            if (count != recordCount) {
                log.error("************************** count of inserts {} does not match expected count {} ************************** ", count, recordCount);
            } else {
                log.info("counts match!!!!");
            }

        }
    }

    public void run(Properties connectionProperties) throws IOException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        Properties cockroachProperties = new Properties();
        cockroachProperties.load(AbstractBatchInsert.class.getClassLoader().getResourceAsStream("cockroach.properties"));

        final String url = cockroachProperties.getProperty("jdbc.url");
        final int batchSize = Integer.parseInt(cockroachProperties.getProperty("jdbc.batch.size", "5"));
        final int recordCount = Integer.parseInt(cockroachProperties.getProperty("record.count", "1000"));

        Random random = new Random();

        try (Connection connection = DriverManager.getConnection(url, connectionProperties)) {
            executeUpdate(connection, DROP_TABLE);

            executeUpdate(connection, CREATE_TABLE);

            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {

                for (int i = 0; i < recordCount; i++) {
                    statement.setInt(1, random.nextInt());
                    statement.addBatch();

                    if (((i + 1) % batchSize) == 0) {
                        saveBatch(connection, statement);
                    }

                }

                log.debug("saving possible hanging batch statements...");

                saveBatch(connection, statement);

            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }

            connection.setAutoCommit(true);

            verifyCount(recordCount, connection);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void saveBatch(Connection connection, PreparedStatement statement) throws SQLException {

        Transaction transaction = conn -> {

            log.debug("executeBatch(): starting");
            int[] counts = statement.executeBatch();
            log.debug("executeBatch(): successful");


            printCounts(counts);

            // execute batch in PG driver calls clear so this is redundant
            /*
            log.debug("attempting to clear batch...");
            statement.clearBatch();
            log.debug("clear batch successful!");
            */

        };

        commit(connection,transaction);

    }

    abstract void commit(Connection connection, Transaction transaction) throws SQLException;


}
