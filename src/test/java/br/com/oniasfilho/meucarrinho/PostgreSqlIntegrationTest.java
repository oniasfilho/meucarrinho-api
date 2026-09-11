package br.com.oniasfilho.meucarrinho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.session.ShoppingSession;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionRepository;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionService;
import br.com.oniasfilho.meucarrinho.session.dto.CreateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemService;
import br.com.oniasfilho.meucarrinho.session.item.dto.CreateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import br.com.oniasfilho.meucarrinho.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PostgreSqlIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ShoppingSessionService sessionService;

    @Autowired
    private ShoppingSessionItemService itemService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ShoppingSessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayJpaAndServiceMutationWorkTogetherAgainstPostgreSql() {
        Integer migratedTables = jdbcTemplate.queryForObject("""
            select count(*)
            from information_schema.tables
            where table_schema = 'public'
              and table_name in ('app_user', 'shopping_session', 'shopping_session_item')
            """, Integer.class);
        assertThat(migratedTables).isEqualTo(3);

        ShoppingSessionDetailResponse created = sessionService.create(
            new CreateShoppingSessionRequest("Teste integrado", null, new BigDecimal("50.00"))
        );
        ShoppingSessionDetailResponse withItem = itemService.create(
            created.id(),
            new CreateShoppingSessionItemRequest(
                "Arroz", new BigDecimal("28.90"), 1, null
            )
        );

        assertThat(withItem.itemCount()).isEqualTo(1);
        assertThat(withItem.total()).isEqualByComparingTo("28.90");
        Integer persistedItems = jdbcTemplate.queryForObject(
            "select count(*) from shopping_session_item where session_id = ?",
            Integer.class,
            created.id()
        );
        assertThat(persistedItems).isEqualTo(1);
    }

    @Test
    void currentUserCannotReadAnotherUsersSession() {
        AppUser anotherUser = userRepository.save(new AppUser(
            UUID.randomUUID(), "another-subject", "other@example.test", "Other User"
        ));
        ShoppingSession anotherUsersSession = sessionRepository.save(new ShoppingSession(
            UUID.randomUUID(),
            anotherUser,
            "Sessão privada",
            null,
            null
        ));

        assertThatThrownBy(() -> sessionService.getDetail(anotherUsersSession.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
