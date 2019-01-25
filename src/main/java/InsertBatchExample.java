import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;

public class InsertBatchExample {


    public static void main(String[] args) throws IOException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS accounts");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT)");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO accounts(balance) VALUES(?)")) {

                for (int i = 0; i < recordCount; i++) {
                    statement.setInt(1, random.nextInt());
                    statement.addBatch();

                    if ((i % batchSize) == 0) {
                        int[] counts = statement.executeBatch();

                        printCounts(counts);

                        statement.clearBatch();
                        connection.commit();
                    }

                }

                int[] counts = statement.executeBatch();

                printCounts(counts);

                statement.clearBatch();
                connection.commit();

            } catch (SQLException e) {
                e.printStackTrace();
            }

            connection.setAutoCommit(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void printCounts(int[] counts) {
        System.out.println("counts size = " + counts.length);

        for (int count : counts) {
            if (count == -2) {
                System.out.println("batch SUCCESS_NO_INFO");
            } else if (count == -3) {
                System.out.println("batch EXECUTE_FAILED");
            } else {
                System.out.println("batch update count = " + count);
            }
        }
    }
}
