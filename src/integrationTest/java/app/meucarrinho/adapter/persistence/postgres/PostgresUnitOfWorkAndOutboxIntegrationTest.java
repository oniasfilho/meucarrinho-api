package app.meucarrinho.adapter.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.bootstrap.MeuCarrinhoApplication;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.testfixtures.TestIds;
import app.meucarrinho.testfixtures.common.DomainEventPublisherContract;
import app.meucarrinho.testfixtures.common.UnitOfWorkContract;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.event.TransactionalEventListener;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = MeuCarrinhoApplication.class)
@Import(PostgresUnitOfWorkAndOutboxIntegrationTest.ListenerConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PostgresUnitOfWorkAndOutboxIntegrationTest implements UnitOfWorkContract, DomainEventPublisherContract {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.modulith.events.jdbc.schema-initialization.enabled", () -> false);
        properties.add("spring.aop.proxy-target-class", () -> false);
    }

    @Autowired
    private UnitOfWork unitOfWork;

    @Autowired
    private DomainEventPublisher events;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicationListener listener;

    @Override
    public UnitOfWork unitOfWork() {
        return unitOfWork;
    }

    @Override
    public DomainEventPublisher domainEventPublisher() {
        listener.clear();
        return events;
    }

    @Override
    public List<DomainEvent> observedEvents() {
        return listener.observedEvents();
    }

    @BeforeEach
    void clearPublications() {
        listener.clear();
        jdbc.update("delete from event_publication");
        jdbc.update("delete from accounts");
    }

    @Test
    void transaction_template_rolls_back_data_and_registered_event_publications_together() {
        var event = new DomainEvent.ListCreated(
                TestIds.listId(), TestIds.accountId(), false, false,
                ActorRef.account(TestIds.accountId()), Instant.parse("2026-09-29T12:00:00Z"));

        assertThatThrownBy(() -> unitOfWork.execute(() -> {
                    jdbc.update("insert into accounts (id, identity_provider, identity_subject, display_name, "
                                    + "sort_order, collaboration_alerts, haptics, analytics_opt_out, currency, "
                                    + "created_at, version) values (?, 'test', 'subject', 'Test', 'ADDED', "
                                    + "false, false, false, 'BRL', now(), 1)",
                            TestIds.uuid());
                    events.publish(List.of(event));
                    assertThat(jdbc.queryForObject("select count(*) from event_publication", Integer.class)).isEqualTo(1);
                    throw new IllegalStateException("force rollback");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("force rollback");

        assertThat(jdbc.queryForObject("select count(*) from accounts", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from event_publication", Integer.class)).isZero();
    }

    @Test
    void committed_domain_event_is_registered_in_the_jdbc_publication_log() {
        var event = new DomainEvent.ListCreated(
                TestIds.listId(), TestIds.accountId(), false, false,
                ActorRef.account(TestIds.accountId()), Instant.parse("2026-09-29T12:00:00Z"));

        unitOfWork.execute(() -> {
            events.publish(List.of(event));
            assertThat(jdbc.queryForObject("select count(*) from event_publication", Integer.class)).isEqualTo(1);
            return null;
        });

        assertThat(jdbc.queryForObject("select count(*) from event_publication", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select completion_date from event_publication", java.time.OffsetDateTime.class))
                .isNotNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ListenerConfiguration {
        @Bean
        PublicationListener publicationListener() {
            return new PublicationListener();
        }
    }

    static class PublicationListener {
        private final List<DomainEvent> observed = new java.util.concurrent.CopyOnWriteArrayList<>();

        @EventListener
        void observe(DomainEvent event) {
            observed.add(event);
        }

        @TransactionalEventListener
        void on(DomainEvent event) {
            // The test listener gives the registry a transactional subscriber to record.
        }

        List<DomainEvent> observedEvents() {
            return List.copyOf(observed);
        }

        void clear() {
            observed.clear();
        }
    }
}
