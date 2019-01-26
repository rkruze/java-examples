package com.cockroach.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractBatchInsert extends AbstractInsert {

    final Logger log = LoggerFactory.getLogger(getClass());

    static final String SAVEPOINT_NAME = "cockroach_restart";

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


    @Override
    void insert(int recordCount, int batchSize, Connection connection) throws SQLException {

        connection.setAutoCommit(false);

        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {

            for (int i = 1; i <= recordCount; i++) {
                statement.setInt(1, RANDOM.nextInt());
                statement.addBatch();

                if ((i % batchSize) == 0) {
                    saveBatch(connection, statement);
                }

            }

            log.debug("saving possible hanging batch statements...");

            saveBatch(connection, statement);

        } finally {
            connection.setAutoCommit(true);
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

        commit(connection, transaction);

    }

    abstract void commit(Connection connection, Transaction transaction) throws SQLException;


}
