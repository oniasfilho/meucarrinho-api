package app.meucarrinho.adapter.persistence.postgres;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

final class PostgresTestDatabase {
    private PostgresTestDatabase() {}

    static JdbcTemplate migrate(PostgreSQLContainer<?> postgres) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        return new JdbcTemplate(dataSource);
    }

    static void clear(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE receipt_lines, receipt_participants, receipts, shopping_list_items, "
                + "shopping_list_members, shopping_lists, accounts CASCADE");
    }
}
