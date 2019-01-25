import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BaseExample {


    private static final Logger log = LoggerFactory.getLogger(BaseExample.class);


    static void executeStatement(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
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
}
