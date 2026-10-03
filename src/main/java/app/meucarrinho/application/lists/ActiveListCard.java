package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.ListTotals;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.StoreName;
import java.time.Instant;
import java.util.Optional;

/** A home card as the API sees it; mirrors {@code port.ListCard}, which the API may not import (ADR 0006). */
public record ActiveListCard(
        ListId id,
        ListName name,
        Optional<StoreName> store,
        ListStatus status,
        ListTotals totals,
        int people,
        Instant updatedAt) {}
