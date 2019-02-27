package com.cockroach.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;

public class BatchInsertWithRetryExample2 extends AbstractInsert {

    @Override
    void insert(int recordCount, int batchSize, Connection connection) throws SQLException {

        while (true) {

            connection.setAutoCommit(false);

            log.debug("begin autocommit {}", connection.getAutoCommit());

            Savepoint savepoint = connection.setSavepoint(SAVEPOINT_NAME);

            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {

                for (int i = 1; i <= recordCount; i++) {
                    statement.setInt(1, RANDOM.nextInt());
                    statement.addBatch();

                    if ((i % batchSize) == 0) {

                        log.debug("executing batch for record {}", i);
                        statement.executeBatch();

                        log.debug("clearing batch for record {}", i);
                        statement.clearBatch();
                    }

                }

                log.debug("executing remaining batch...");
                statement.executeBatch();

                log.debug("clearing batch...");
                statement.clearBatch();

                log.debug("releasing savepoint...");
                connection.releaseSavepoint(savepoint);

                log.debug("committing...");
                connection.commit();

                break;

            } catch (SQLException e) {

                String sqlState = e.getSQLState();

                log.error(String.format("error trying to commit with sql state [%s]: %s", sqlState, e.getMessage()), e);

                if (sqlState.equals("40001")) {

                    log.debug("attempting rollback...");
                    connection.rollback(savepoint);
                } else {
                    throw e;
                }


            } finally {

                connection.setAutoCommit(true);
                log.debug("end autocommit {}", connection.getAutoCommit());
            }

        }
    }
}
