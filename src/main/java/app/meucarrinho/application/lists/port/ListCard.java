package app.meucarrinho.application.lists.port;

import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.ListTotals;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.StoreName;
import java.time.Instant;
import java.util.Optional;

public record ListCard(
        ListId id,
        ListName name,
        Optional<StoreName> store,
        ListStatus status,
        ListTotals totals,
        int people,
        Instant updatedAt) {}
