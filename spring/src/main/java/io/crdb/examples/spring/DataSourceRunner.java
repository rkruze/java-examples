package io.crdb.examples.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

@Component
@Profile("ds")
public class DataSourceRunner implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String INSERT = "INSERT INTO spring_example(balance) VALUES(?)";
    private static final String SELECT_COUNT = "select count(*) from spring_example";

    private static final Random RANDOM = new Random();

    private DataSource dataSource;
    private Environment environment;

    @Autowired
    public DataSourceRunner(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        int batchSize = environment.getProperty("example.batch.size", Integer.class, 128);
        int recordCount = environment.getProperty("example.record.count", Integer.class, 1000);


        try (Connection connection = dataSource.getConnection()) {

            // insert records

            try (final PreparedStatement insert = connection.prepareStatement(INSERT)) {

                for (int i = 1; i <= recordCount; i++) {
                    insert.setInt(1, RANDOM.nextInt());
                    insert.addBatch();

                    if ((i % batchSize) == 0) {
                        insert.executeBatch();
                    }

                }

                insert.executeBatch();
            }

            // query to verify insert

            try (Statement select = connection.createStatement();
                 ResultSet rs = select.executeQuery(SELECT_COUNT)) {

                int count = 0;

                if (rs.next()) {
                    count = rs.getInt(1);
                }

                if (count != recordCount) {
                    throw new RuntimeException(String.format("count of inserts [%d] does not match expected count [%d]", count, recordCount));
                }
            }

        }

    }
}
