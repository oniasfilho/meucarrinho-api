package br.com.oniasfilho.meucarrinho.session.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import br.com.oniasfilho.meucarrinho.common.error.BusinessRuleException;
import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService;
import br.com.oniasfilho.meucarrinho.session.ShoppingSession;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionMapper;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionRepository;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.dto.CreateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.UpdateItemQuantityRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.UpdateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import br.com.oniasfilho.meucarrinho.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingSessionItemServiceTest {

    @Mock
    private ShoppingSessionRepository sessionRepository;

    @Mock
    private ShoppingSessionItemRepository itemRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AppUser user;
    private ShoppingSession session;
    private ShoppingSessionItemService service;

    @BeforeEach
    void setUp() {
        user = new AppUser(UUID.randomUUID(), "local-demo-user", null, "Demo");
        session = new ShoppingSession(
            UUID.randomUUID(), user, "Compra", null, new BigDecimal("100.00")
        );
        ShoppingSessionMapper mapper = new ShoppingSessionMapper(
            new ShoppingSessionItemMapper(mock(ObjectStorageService.class))
        );
        service = new ShoppingSessionItemService(
            sessionRepository, itemRepository, currentUserService, mapper
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void createsItemAndReturnsRecalculatedDetail() {
        ownedSessionIsFound();
        AtomicReference<ShoppingSessionItem> saved = new AtomicReference<>();
        when(itemRepository.save(any())).thenAnswer(invocation -> {
            ShoppingSessionItem item = invocation.getArgument(0);
            saved.set(item);
            return item;
        });
        when(itemRepository.findAllBySessionIdOrderByCreatedAtDesc(session.getId()))
            .thenAnswer(ignored -> List.of(saved.get()));

        ShoppingSessionDetailResponse response = service.create(
            session.getId(),
            new CreateShoppingSessionItemRequest(
                " Arroz ", new BigDecimal("28.90"), 2, null
            )
        );

        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.total()).isEqualByComparingTo("57.80");
        assertThat(response.items()).singleElement().extracting("name").isEqualTo("Arroz");
    }

    @Test
    void updatesAllEditableItemFields() {
        ownedSessionIsFound();
        ShoppingSessionItem item = item();
        when(itemRepository.findByIdAndSessionId(item.getId(), session.getId()))
            .thenReturn(Optional.of(item));
        when(itemRepository.findAllBySessionIdOrderByCreatedAtDesc(session.getId()))
            .thenReturn(List.of(item));

        ShoppingSessionDetailResponse response = service.update(
            session.getId(),
            item.getId(),
            new UpdateShoppingSessionItemRequest(
                "Feijão", new BigDecimal("8.49"), 2, "Tipo 1"
            )
        );

        assertThat(response.total()).isEqualByComparingTo("16.98");
        assertThat(response.items()).singleElement().satisfies(updated -> {
            assertThat(updated.name()).isEqualTo("Feijão");
            assertThat(updated.quantity()).isEqualTo(2);
            assertThat(updated.note()).isEqualTo("Tipo 1");
        });
    }

    @Test
    void updatesFocusedQuantity() {
        ownedSessionIsFound();
        ShoppingSessionItem item = item();
        when(itemRepository.findByIdAndSessionId(item.getId(), session.getId()))
            .thenReturn(Optional.of(item));
        when(itemRepository.findAllBySessionIdOrderByCreatedAtDesc(session.getId()))
            .thenReturn(List.of(item));

        ShoppingSessionDetailResponse response = service.updateQuantity(
            session.getId(), item.getId(), new UpdateItemQuantityRequest(3)
        );

        assertThat(response.itemCount()).isEqualTo(3);
        assertThat(response.total()).isEqualByComparingTo("30.00");
    }

    @Test
    void rejectsQuantityZeroEvenWhenServiceIsCalledDirectly() {
        ownedSessionIsFound();
        ShoppingSessionItem item = item();
        when(itemRepository.findByIdAndSessionId(item.getId(), session.getId()))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.updateQuantity(
            session.getId(), item.getId(), new UpdateItemQuantityRequest(0)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletesItemThatBelongsToOwnedActiveSession() {
        ownedSessionIsFound();
        ShoppingSessionItem item = item();
        when(itemRepository.findByIdAndSessionId(item.getId(), session.getId()))
            .thenReturn(Optional.of(item));

        service.delete(session.getId(), item.getId());

        verify(itemRepository).delete(item);
    }

    @Test
    void rejectsEveryItemMutationForCompletedSession() {
        session.complete(Instant.parse("2026-09-01T03:00:00Z"));
        ownedSessionIsFound();

        assertThatThrownBy(() -> service.create(
            session.getId(),
            new CreateShoppingSessionItemRequest("Item", BigDecimal.ONE, 1, null)
        )).isInstanceOf(BusinessRuleException.class);
        verify(itemRepository, never()).save(any());
    }

    private void ownedSessionIsFound() {
        when(sessionRepository.findByIdAndUserId(session.getId(), user.getId()))
            .thenReturn(Optional.of(session));
    }

    private ShoppingSessionItem item() {
        return new ShoppingSessionItem(
            UUID.randomUUID(), session, "Item", new BigDecimal("10.00"), 1, null, null
        );
    }
}
