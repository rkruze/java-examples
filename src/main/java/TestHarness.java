import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class TestHarness {

    private static final Logger log = LoggerFactory.getLogger(TestHarness.class);
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
        connectionProperties.setProperty("reWriteBatchedInserts", "true");


        if (test.equals(BATCH_INSERT_TEST)) {
            try {

                connectionProperties.setProperty("ApplicationName", "BatchInsertExample");

                new BatchInsertExample().run(connectionProperties);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (test.equals(BATCH_INSERT_WITH_RETRY)) {
            try {

                connectionProperties.setProperty("ApplicationName", "BatchInsertWithRetryExample");

                new BatchInsertWithRetryExample().run(connectionProperties);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

    }
}
