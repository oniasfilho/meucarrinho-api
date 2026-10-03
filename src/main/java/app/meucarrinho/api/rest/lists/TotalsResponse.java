package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.MoneyDto;
import org.jspecify.annotations.Nullable;

public record TotalsResponse(
        MoneyDto pickedTotal,
        MoneyDto estimatedTotal,
        @Nullable MoneyDto remainingBudget,
        boolean overBudget,
        int pendingCount,
        int pickedCount,
        int unpricedCount) {}
