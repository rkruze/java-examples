import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestHarness {

    private static final Logger log = LoggerFactory.getLogger(TestHarness.class);
    private static final String BATCH_INSERT_TEST = "bi";
    private static final String BATCH_INSERT_WITH_RETRY = "bir";


    public static void main(String[] args) {

        String test = BATCH_INSERT_TEST;

        if (args != null && args.length != 0) {
            test = args[0];
        }


        if (test.equals(BATCH_INSERT_TEST)) {
            try {
                new BatchInsertExample().run();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (test.equals(BATCH_INSERT_WITH_RETRY)) {
            try {
                new BatchInsertWithRetryExample().run();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

    }
}
