package br.com.oniasfilho.meucarrinho.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.BusinessRuleException;
import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService;
import br.com.oniasfilho.meucarrinho.session.dto.CreateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionSummaryResponse;
import br.com.oniasfilho.meucarrinho.session.dto.UpdateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItem;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemMapper;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemRepository;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import br.com.oniasfilho.meucarrinho.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T04:00:00Z");

    @Mock
    private ShoppingSessionRepository sessionRepository;

    @Mock
    private ShoppingSessionItemRepository itemRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AppUser user;
    private ShoppingSessionService service;

    @BeforeEach
    void setUp() {
        user = new AppUser(UUID.randomUUID(), "local-demo-user", null, "Demo");
        ShoppingSessionMapper mapper = new ShoppingSessionMapper(
            new ShoppingSessionItemMapper(mock(ObjectStorageService.class))
        );
        service = new ShoppingSessionService(
            sessionRepository,
            itemRepository,
            currentUserService,
            mapper,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void createsAnActiveSession() {
        CreateShoppingSessionRequest request = new CreateShoppingSessionRequest(
            "  Mercado  ", "  Loja  ", new BigDecimal("100.00")
        );

        ShoppingSessionDetailResponse response = service.create(request);

        ArgumentCaptor<ShoppingSession> captor = ArgumentCaptor.forClass(ShoppingSession.class);
        verify(sessionRepository).save(captor.capture());
        ShoppingSession saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Mercado");
        assertThat(saved.getStoreName()).isEqualTo("Loja");
        assertThat(saved.getStatus()).isEqualTo(ShoppingSessionStatus.ACTIVE);
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void listsOnlyActiveSessionsAndDerivesSummariesInTwoQueries() {
        ShoppingSession session = activeSession();
        ShoppingSessionItem item = item(session, "Arroz", "9.50", 2);
        when(sessionRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
            user.getId(), ShoppingSessionStatus.ACTIVE
        )).thenReturn(List.of(session));
        when(itemRepository.findAllBySessionIdInOrderByCreatedAtDesc(List.of(session.getId())))
            .thenReturn(List.of(item));

        List<ShoppingSessionSummaryResponse> response = service.list(ShoppingSessionStatus.ACTIVE);

        assertThat(response).singleElement().satisfies(summary -> {
            assertThat(summary.itemCount()).isEqualTo(2);
            assertThat(summary.total()).isEqualByComparingTo("19.00");
        });
        verify(sessionRepository, never()).findAllByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getsDetailScopedToCurrentUser() {
        ShoppingSession session = activeSession();
        when(sessionRepository.findByIdAndUserId(session.getId(), user.getId()))
            .thenReturn(Optional.of(session));
        when(itemRepository.findAllBySessionIdOrderByCreatedAtDesc(session.getId()))
            .thenReturn(List.of());

        ShoppingSessionDetailResponse response = service.getDetail(session.getId());

        assertThat(response.id()).isEqualTo(session.getId());
        verify(sessionRepository).findByIdAndUserId(session.getId(), user.getId());
        verify(sessionRepository, never()).findById(session.getId());
    }

    @Test
    void returnsNotFoundWhenSessionIsOutsideCurrentUsersScope() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(sessionId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Shopping session was not found.");
    }

    @Test
    void completesAnActiveSessionAndRejectsSecondCompletion() {
        ShoppingSession session = activeSession();
        when(sessionRepository.findByIdAndUserId(session.getId(), user.getId()))
            .thenReturn(Optional.of(session));
        when(itemRepository.findAllBySessionIdOrderByCreatedAtDesc(session.getId()))
            .thenReturn(List.of());

        ShoppingSessionDetailResponse completed = service.complete(session.getId());

        assertThat(completed.status()).isEqualTo(ShoppingSessionStatus.COMPLETED);
        assertThat(completed.completedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.complete(session.getId()))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsSettingsChangesAfterCompletion() {
        ShoppingSession session = completedSession();
        when(sessionRepository.findByIdAndUserId(session.getId(), user.getId()))
            .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.update(
            session.getId(),
            new UpdateShoppingSessionRequest("Novo nome", null, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deletesOwnedSession() {
        ShoppingSession session = activeSession();
        when(sessionRepository.findByIdAndUserId(session.getId(), user.getId()))
            .thenReturn(Optional.of(session));

        service.delete(session.getId());

        verify(sessionRepository).delete(session);
    }

    @Test
    void duplicatesSessionAndItemsUsingNewIds() {
        ShoppingSession original = completedSession();
        ShoppingSessionItem originalItem = item(original, "Café", "18.90", 2);
        when(sessionRepository.findByIdAndUserId(original.getId(), user.getId()))
            .thenReturn(Optional.of(original));
        when(itemRepository.findAllBySessionIdOrderByCreatedAtDesc(original.getId()))
            .thenReturn(List.of(originalItem));

        ShoppingSessionDetailResponse copy = service.duplicate(original.getId());

        assertThat(copy.id()).isNotEqualTo(original.getId());
        assertThat(copy.name()).isEqualTo(original.getName() + " (cópia)");
        assertThat(copy.status()).isEqualTo(ShoppingSessionStatus.ACTIVE);
        assertThat(copy.completedAt()).isNull();
        assertThat(copy.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isNotEqualTo(originalItem.getId());
            assertThat(item.name()).isEqualTo(originalItem.getName());
            assertThat(item.quantity()).isEqualTo(originalItem.getQuantity());
        });
        verify(itemRepository).saveAll(any());
    }

    private ShoppingSession activeSession() {
        return new ShoppingSession(
            UUID.randomUUID(), user, "Compra", "Loja", new BigDecimal("100.00")
        );
    }

    private ShoppingSession completedSession() {
        ShoppingSession session = activeSession();
        session.complete(NOW.minusSeconds(60));
        return session;
    }

    private ShoppingSessionItem item(
        ShoppingSession session,
        String name,
        String price,
        int quantity
    ) {
        return new ShoppingSessionItem(
            UUID.randomUUID(), session, name, new BigDecimal(price), quantity, null, null
        );
    }
}

