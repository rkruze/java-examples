package io.crdb.examples.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("jdbc1")
public class JdbcTemplateRunner implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String INSERT = "INSERT INTO spring_example(balance) VALUES(?)";
    private static final String SELECT_COUNT = "select count(*) from spring_example";

    private static final Random RANDOM = new Random();

    private JdbcTemplate jdbcTemplate;
    private Environment environment;

    @Autowired
    public JdbcTemplateRunner(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        int batchSize = environment.getProperty("example.batch.size", Integer.class, 128);
        int recordCount = environment.getProperty("example.record.count", Integer.class, 1000);

        // Generate dummy data...
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= recordCount; i++) {
            data.add(RANDOM.nextInt());
        }

        // Batch insert dummy data into CockroachDB
        jdbcTemplate.batchUpdate(INSERT, data, batchSize, (ps, argument) -> ps.setInt(1, argument));

        // Verify insert was successful
        Integer count = jdbcTemplate.queryForObject(SELECT_COUNT, Integer.class);

        if (count == null || count != recordCount) {
            throw new RuntimeException(String.format("count of inserts [%d] does not match expected count [%d]", count, recordCount));
        }
    }
}
