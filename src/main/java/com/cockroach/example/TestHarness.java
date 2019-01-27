package com.cockroach.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class TestHarness {

    private static final Logger log = LoggerFactory.getLogger(TestHarness.class);
    private static final String INSERT_TEST = "i";
    private static final String BATCH_INSERT_TEST = "bi";
    private static final String BATCH_INSERT_WITH_RETRY = "bir";

    public static void main(String[] args) {

        String test = BATCH_INSERT_TEST;

        if (args != null && args.length != 0) {
            test = args[0];
        }


        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("sslmode", "disable");

        // this increases insert performance considerably
        connectionProperties.setProperty("reWriteBatchedInserts", "true");

        String name = null;
        AbstractInsert insert = null;

        switch (test) {
            case INSERT_TEST:
                name = "InsertExample";
                insert = new InsertExample();
                break;
            case BATCH_INSERT_TEST:
                name = "BatchInsertExample";
                insert = new BatchInsertExample();
                break;
            case BATCH_INSERT_WITH_RETRY:
                name = "BatchInsertWithRetryExample";
                insert = new BatchInsertWithRetryExample();
                break;
        }


        int failureCount = 0;
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            log.debug("**************************************** test {}: run {} ****************************************", name, i);

            try {
                connectionProperties.setProperty("ApplicationName", name);
                boolean failed = insert.run(connectionProperties);

                if (failed) {
                    failureCount++;
                }

            } catch (Exception e) {
                log.error(String.format("error calling run (shouldn't get here): %s", e.getMessage()), e);
            }
        }

        float failureRate = ((float) failureCount / iterations) * 100;
        log.debug("failure rate = {}", failureRate);


    }
}
