package com.cockroach.example;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class AbstractInsert {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS accounts";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT)";
    private static final String SELECT_COUNT = "select count(*) from accounts";

    static final String INSERT = "INSERT INTO accounts(balance) VALUES(?)";

    static final Random RANDOM = new Random();

    AbstractInsert() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    boolean run(Properties connectionProperties) throws IOException {

        final AtomicBoolean failed = new AtomicBoolean(false);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        final Properties cockroachProperties = new Properties();
        cockroachProperties.load(AbstractInsert.class.getClassLoader().getResourceAsStream("cockroach.properties"));

        final String url = cockroachProperties.getProperty("jdbc.url");
        final int batchSize = Integer.parseInt(cockroachProperties.getProperty("jdbc.batch.size", "5"));
        final int recordCount = Integer.parseInt(cockroachProperties.getProperty("record.count", "1000"));
        final int pause = Integer.parseInt(cockroachProperties.getProperty("pause.duration", "0"));


        Timer timer = Timer
                .builder("insert")
                .register(Metrics.globalRegistry);

        try (Connection connection = DriverManager.getConnection(url, connectionProperties)) {

            log.info("TRANSACTION ISOLATION: {}", connection.getTransactionIsolation());

            if (connection.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE) {
                throw new IllegalArgumentException("transaction isolation must be Connection.TRANSACTION_SERIALIZABLE");
            }

            timer.record(() -> executeUpdate(connection, DROP_TABLE));

            log.info("drop table time: {} ms or {} ns", timer.totalTime(TimeUnit.MILLISECONDS), timer.totalTime(TimeUnit.NANOSECONDS));

            timer.record(() -> executeUpdate(connection, CREATE_TABLE));

            log.info("create table time: {} ms or {} ns", timer.totalTime(TimeUnit.MILLISECONDS), timer.totalTime(TimeUnit.NANOSECONDS));

            if (pause > 0) {
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException ignore) {

                }
            }

            timer.record(() -> {
                try {
                    insert(recordCount, batchSize, connection);
                } catch (SQLException e) {
                    failed.set(true);
                    log.error(String.format("error inserting: %s", e.getMessage()), e);
                }
            });

            log.info("insert time: {} seconds or {} ms", timer.totalTime(TimeUnit.SECONDS), timer.totalTime(TimeUnit.MILLISECONDS));

            verifyCount(recordCount, connection);

        } catch (Exception e) {
            log.error(String.format("error in run: %s", e.getMessage()), e);
            failed.set(true);
        }

        return failed.get();
    }

    private void executeUpdate(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            final int i = statement.executeUpdate(sql);
            log.debug("updated {} records", i);
        } catch (SQLException e) {
            log.error(String.format("error executing update: %s", e.getMessage()), e);
        }
    }


    private void verifyCount(int recordCount, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_COUNT)) {

            int count = 0;

            if (rs.next()) {
                count = rs.getInt(1);
            }

            if (count != recordCount) {
                throw new RuntimeException(String.format("count of inserts [%d] does not match expected count [%d]", count, recordCount));
            }
        }
    }

    abstract void insert(int recordCount, int batchSize, Connection connection) throws SQLException;

}
