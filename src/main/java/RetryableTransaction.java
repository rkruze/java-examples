import java.sql.Connection;
import java.sql.SQLException;

public interface RetryableTransaction {


    void attemptTransaction(Connection conn) throws SQLException;

}
