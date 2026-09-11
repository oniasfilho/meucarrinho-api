package br.com.oniasfilho.meucarrinho.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.ShoppingSession;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionRepository;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItem;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemRepository;
import br.com.oniasfilho.meucarrinho.user.dto.AccountStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountStatsServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ShoppingSessionRepository sessionRepository;

    @Mock
    private ShoppingSessionItemRepository itemRepository;

    private AppUser user;
    private AccountStatsService service;

    @BeforeEach
    void setUp() {
        user = new AppUser(UUID.randomUUID(), "local-demo-user", null, "Demo");
        service = new AccountStatsService(currentUserService, sessionRepository, itemRepository);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void calculatesCountsAndSpendingFromCompletedSessionsOnly() {
        ShoppingSession completed = new ShoppingSession(
            UUID.randomUUID(), user, "Concluída", null, null
        );
        when(sessionRepository.countByUser_Id(user.getId())).thenReturn(3L);
        when(sessionRepository.countByUser_IdAndStatus(user.getId(), ShoppingSessionStatus.COMPLETED))
            .thenReturn(1L);
        when(itemRepository.findAllByUserIdAndSessionStatus(
            user.getId(), ShoppingSessionStatus.COMPLETED
        )).thenReturn(List.of(
            new ShoppingSessionItem(
                UUID.randomUUID(), completed, "Arroz", new BigDecimal("28.90"), 1, null, null
            ),
            new ShoppingSessionItem(
                UUID.randomUUID(), completed, "Feijão", new BigDecimal("8.49"), 2, null, null
            )
        ));

        AccountStatsResponse response = service.getStats();

        assertThat(response.sessionCount()).isEqualTo(3);
        assertThat(response.completedSessionCount()).isEqualTo(1);
        assertThat(response.totalRecordedSpending()).isEqualByComparingTo("45.88");
    }
}

