package app.meucarrinho.adapter.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Test
    void flyway_creates_the_relational_schema_for_in_scope_aggregates() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "select table_name from information_schema.tables "
                                + "where table_schema = 'public' and table_type = 'BASE TABLE'")) {
            Set<String> tables = new java.util.HashSet<>();
            while (result.next()) {
                tables.add(result.getString(1));
            }
            assertThat(tables).contains(
                    "accounts",
                    "shopping_lists",
                    "shopping_list_members",
                    "shopping_list_items",
                    "receipts",
                    "receipt_participants",
                    "receipt_lines");
            assertThat(tables).doesNotContain("invitations", "guest_passes", "change_log", "catalog_entries");
        }
    }
}
