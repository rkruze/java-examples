import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;

public class InsertBatchExample {

    private static final Logger log = LoggerFactory.getLogger(InsertBatchExample.class);

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS accounts";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT)";
    private static final String INSERT = "INSERT INTO accounts(balance) VALUES(?)";


    public static void main(String[] args) throws IOException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        Properties cockroachProperties = new Properties();
        cockroachProperties.load(InsertBatchExample.class.getClassLoader().getResourceAsStream("cockroach.properties"));

        final String url = cockroachProperties.getProperty("jdbc.url");
        final int batchSize = Integer.parseInt(cockroachProperties.getProperty("jdbc.batch.size", "5"));
        final int recordCount = Integer.parseInt(cockroachProperties.getProperty("record.count", "1000"));

        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("sslmode", "disable");
        connectionProperties.setProperty("ApplicationName", InsertBatchExample.class.toString());
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

                    if ( ((i + 1) % batchSize) == 0) {
                        int[] counts = statement.executeBatch();

                        printCounts(counts);

                        statement.clearBatch();
                        connection.commit();
                    }

                }

                log.debug("executing cleanup...");

                int[] counts = statement.executeBatch();

                printCounts(counts);

                statement.clearBatch();
                connection.commit();

            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }

            connection.setAutoCommit(true);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void executeStatement(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void printCounts(int[] counts) {
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
}
