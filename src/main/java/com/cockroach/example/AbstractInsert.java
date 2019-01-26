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

public abstract class AbstractInsert {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS accounts";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT)";
    static final String INSERT = "INSERT INTO accounts(balance) VALUES(?)";

    static final Random RANDOM = new Random();

    public AbstractInsert() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    void executeUpdate(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }


    void verifyCount(int recordCount, Connection connection) throws SQLException {
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
        cockroachProperties.load(AbstractInsert.class.getClassLoader().getResourceAsStream("cockroach.properties"));

        final String url = cockroachProperties.getProperty("jdbc.url");
        final int batchSize = Integer.parseInt(cockroachProperties.getProperty("jdbc.batch.size", "5"));
        final int recordCount = Integer.parseInt(cockroachProperties.getProperty("record.count", "1000"));


        Timer timer = Timer
                .builder("insert")
                .register(Metrics.globalRegistry);

        try (Connection connection = DriverManager.getConnection(url, connectionProperties)) {

            log.info("TRANSACTION ISOLATION: {}", connection.getTransactionIsolation());

            if (connection.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE) {
                throw new IllegalArgumentException("transaction isolation must be Connection.TRANSACTION_SERIALIZABLE");
            }

            executeUpdate(connection, DROP_TABLE);

            executeUpdate(connection, CREATE_TABLE);

            timer.record(() -> {
                try {
                    insert(recordCount, batchSize, connection);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            });

            log.info("insert time {} seconds", timer.totalTime(TimeUnit.SECONDS));

            verifyCount(recordCount, connection);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    abstract void insert(int recordCount, int batchSize, Connection connection) throws SQLException;

}
