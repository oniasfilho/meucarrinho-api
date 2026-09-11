package br.com.oniasfilho.meucarrinho.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItem;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemMapper;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import org.junit.jupiter.api.Test;

class ShoppingSessionMapperTest {

    private final ShoppingSessionMapper mapper = new ShoppingSessionMapper(
        new ShoppingSessionItemMapper(mock(ObjectStorageService.class))
    );

    @Test
    void derivesItemCountAndTotalFromQuantitiesAndPrices() {
        AppUser user = new AppUser(UUID.randomUUID(), "subject", null, "User");
        ShoppingSession session = new ShoppingSession(
            UUID.randomUUID(), user, "Compra", null, new BigDecimal("50.00")
        );
        List<ShoppingSessionItem> items = List.of(
            item(session, "Arroz", "28.90", 1),
            item(session, "Feijão", "8.49", 2)
        );

        ShoppingSessionDetailResponse response = mapper.toDetail(session, items);

        assertThat(response.itemCount()).isEqualTo(3);
        assertThat(response.total()).isEqualByComparingTo("45.88");
        assertThat(response.remainingBudget()).isEqualByComparingTo("4.12");
        assertThat(response.overBudget()).isFalse();
    }

    @Test
    void reportsOverBudgetWithoutPersistingDerivedValues() {
        AppUser user = new AppUser(UUID.randomUUID(), "subject", null, "User");
        ShoppingSession session = new ShoppingSession(
            UUID.randomUUID(), user, "Compra", null, new BigDecimal("10.00")
        );

        ShoppingSessionDetailResponse response = mapper.toDetail(
            session,
            List.of(item(session, "Item", "6.00", 2))
        );

        assertThat(response.total()).isEqualByComparingTo("12.00");
        assertThat(response.remainingBudget()).isEqualByComparingTo("-2.00");
        assertThat(response.overBudget()).isTrue();
    }

    private ShoppingSessionItem item(
        ShoppingSession session,
        String name,
        String unitPrice,
        int quantity
    ) {
        return new ShoppingSessionItem(
            UUID.randomUUID(), session, name, new BigDecimal(unitPrice), quantity, null, null
        );
    }
}

