import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;

public class InsertBatchWithRetryExample extends BaseExample {

    private static final Logger log = LoggerFactory.getLogger(InsertBatchWithRetryExample.class);

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS accounts";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT)";
    private static final String INSERT = "INSERT INTO accounts(balance) VALUES(?)";
    private static final String SAVEPOINT = "COCKROACH_RESTART";


    public static void main(String[] args) throws IOException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        Properties cockroachProperties = new Properties();
        cockroachProperties.load(InsertBatchWithRetryExample.class.getClassLoader().getResourceAsStream("cockroach.properties"));

        final String url = cockroachProperties.getProperty("jdbc.url");
        final int batchSize = Integer.parseInt(cockroachProperties.getProperty("jdbc.batch.size", "5"));
        final int recordCount = Integer.parseInt(cockroachProperties.getProperty("record.count", "1000"));

        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("sslmode", "disable");
        connectionProperties.setProperty("ApplicationName", InsertBatchWithRetryExample.class.getName());
        connectionProperties.setProperty("reWriteBatchedInserts", "true");

        Random random = new Random();

        try (Connection connection = DriverManager.getConnection(url, connectionProperties)) {
            executeStatement(connection, DROP_TABLE);

            executeStatement(connection, CREATE_TABLE);

            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {

                for (int i = 0; i < recordCount; i++) {
                    statement.setInt(1, random.nextInt());
                    statement.addBatch();

                    if (((i + 1) % batchSize) == 0) {

                        saveBatch(connection, statement);

                    }

                }

                log.debug("executing cleanup...");

                saveBatch(connection, statement);

            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }

            connection.setAutoCommit(true);


        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void saveBatch(Connection connection, PreparedStatement statement) throws SQLException {
        commitWithRetry(connection, new RetryableTransaction() {
            @Override
            public void attemptTransaction(Connection conn) throws SQLException {

                log.debug("attempting to execute batch");
                int[] counts = statement.executeBatch();

                printCounts(counts);

                log.debug("execute batch successful!!!");

            }
        });


        log.debug("attempting to clear batch");
        statement.clearBatch();
        log.debug("clear batch successful!!!");
    }

    private static void commitWithRetry(Connection connection, RetryableTransaction tx) throws SQLException {

        Savepoint savepoint = connection.setSavepoint(SAVEPOINT);

        int retryCounter = 1;

        while (true) {



            boolean releaseAttempted = false;

            try {
                log.debug("attempting commit: {}", retryCounter);
                tx.attemptTransaction(connection);
                releaseAttempted = true;

                log.debug("attempting to release savepoint: {}", retryCounter);
                connection.releaseSavepoint(savepoint);
                log.debug("savepoint released: {}", retryCounter);
                break;
            } catch (SQLException e) {

                String sqlState = e.getSQLState();

                // Check if the error code indicates a SERIALIZATION_FAILURE.
                if (sqlState.equals("40001")) {
                    // Signal the database that we will attempt a retry.
                    log.error("attempting rollback after [" + e.getMessage() + "]", e);
                    connection.rollback(savepoint);
                    log.debug("rollback successful!!!");
                } else if (releaseAttempted) {
                    throw new RuntimeException("fail during release?", e);
                } else {
                    throw e;
                }
            }

            retryCounter++;
        }

        connection.commit();
    }

}
