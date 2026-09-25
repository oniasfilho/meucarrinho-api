package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.Money;
import java.util.Collection;
import java.util.Optional;

public record ListTotals(
        Money pickedTotal,
        Money estimatedTotal,
        Optional<Money> remainingBudget,
        boolean overBudget,
        int pendingCount,
        int pickedCount,
        int unpricedCount) {
    public static ListTotals of(Collection<Item> items, Optional<Budget> budget) {
        Money picked = Money.ZERO;
        Money estimated = Money.ZERO;
        int pending = 0, pickedCount = 0, unpriced = 0;
        for (Item item : items) {
            if (item.isRemoved()) {
                continue;
            }
            Optional<Money> subtotal = item.subtotal();
            if (subtotal.isEmpty()) {
                unpriced++;
            }
            Money amount = subtotal.orElse(Money.ZERO);
            estimated = estimated.plus(amount);
            if (item.picked()) {
                picked = picked.plus(amount);
                pickedCount++;
            } else {
                pending++;
            }
        }
        Money pickedTotal = picked;
        Optional<Money> remaining = budget.map(b -> b.amount().minus(pickedTotal));
        boolean over = budget.map(b -> pickedTotal.isGreaterThan(b.amount())).orElse(false);
        return new ListTotals(pickedTotal, estimated, remaining, over, pending, pickedCount, unpriced);
    }
}
