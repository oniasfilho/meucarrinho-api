package app.meucarrinho.domain.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReceiptTest {
    private static final Instant NOW = Instant.parse("2026-09-25T15:00:00Z");
    private final ActorRef marina = ActorRef.account(TestIds.accountId());

    private Receipt receipt(List<ReceiptLine> lines, Optional<Budget> budget) {
        return Receipt.of(TestIds.receiptId(), TestIds.listId(), new ListName("Feira"), Optional.empty(), NOW, marina,
                List.of(marina), lines, budget);
    }

    @Test
    void totals_and_budget_delta_come_from_the_lines() {
        Receipt receipt = receipt(List.of(
                ReceiptLine.of(new ItemName("banana"), Quantity.kilograms("1.5"), Optional.of(Money.brl(6_99))),
                ReceiptLine.of(new ItemName("sacola"), Quantity.one(), Optional.empty())), Optional.of(Budget.brl(20_00)));

        assertThat(receipt.total()).isEqualTo(Money.brl(10_49));
        assertThat(receipt.budgetDelta()).contains(Money.brl(9_51));
        assertThat(receipt.overBudget()).isFalse();
        assertThat(receipt.lines().get(1).subtotal()).isEqualTo(Money.ZERO);
    }

    @Test
    void cannot_be_built_with_numbers_that_disagree_with_its_lines() {
        var line = ReceiptLine.of(new ItemName("leite"), Quantity.one(), Optional.of(Money.brl(5_49)));
        assertThatThrownBy(() -> new Receipt(TestIds.receiptId(), TestIds.listId(), new ListName("x"), Optional.empty(),
                NOW, marina, List.of(marina), List.of(line), Money.brl(1_00), Optional.empty(), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> receipt(List.of(), Optional.empty())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void is_a_snapshot_that_later_changes_to_the_source_list_cannot_reach() {
        var lines = new ArrayList<>(List.of(ReceiptLine.of(new ItemName("leite"), Quantity.one(), Optional.of(Money.brl(5_49)))));
        Receipt receipt = receipt(lines, Optional.empty());
        lines.clear();

        assertThat(receipt.lines()).hasSize(1);
        assertThatThrownBy(() -> receipt.lines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
