package app.meucarrinho.domain.receipt;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.StoreName;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record Receipt(
        ReceiptId id,
        ListId listId,
        ListName name,
        Optional<StoreName> store,
        Instant completedAt,
        ActorRef finishedBy,
        List<ActorRef> participants,
        List<ReceiptLine> lines,
        Money total,
        Optional<Budget> budget,
        Optional<Money> budgetDelta) {
    public Receipt {
        participants = List.copyOf(participants);
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("A receipt has at least one line");
        }
        Money sum = lines.stream().map(ReceiptLine::subtotal).reduce(Money.ZERO, Money::plus);
        if (!sum.equals(total)) {
            throw new IllegalArgumentException("Receipt total " + total + " does not match its lines " + sum);
        }
        Money totalPaid = total;
        if (!budgetDelta.equals(budget.map(b -> b.amount().minus(totalPaid)))) {
            throw new IllegalArgumentException("Budget delta does not match budget minus total");
        }
    }

    public static Receipt of(ReceiptId id, ListId listId, ListName name, Optional<StoreName> store,
            Instant completedAt, ActorRef finishedBy, List<ActorRef> participants, List<ReceiptLine> lines,
            Optional<Budget> budget) {
        Money total = lines.stream().map(ReceiptLine::subtotal).reduce(Money.ZERO, Money::plus);
        return new Receipt(id, listId, name, store, completedAt, finishedBy, participants, lines, total, budget,
                budget.map(b -> b.amount().minus(total)));
    }

    public boolean overBudget() {
        return budgetDelta.map(Money::isNegative).orElse(false);
    }

    public boolean isVisibleTo(AccountId account) {
        return participants.contains(ActorRef.account(account));
    }
}
