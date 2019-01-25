import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class BaseExample {


    private static final Logger log = LoggerFactory.getLogger(BaseExample.class);

    private static final String SAVEPOINT = "COCKROACH_RESTART";


    static void executeUpdate(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    static void printCounts(int[] counts) {
        log.debug("counts size = " + counts.length);

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

    static void commitWithRetry(Connection connection, Transaction tx) throws SQLException {

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

    static void commit(Connection connection, Transaction tx) throws SQLException {

        tx.attemptTransaction(connection);

        connection.commit();
    }


    static void verifyCount(int recordCount, Connection connection) throws SQLException {
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
}
