package app.meucarrinho.adapter.persistence.postgres;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

@Table("shopping_list_members")
record ShoppingListMemberRow(
        UUID listId,
        int memberIndex,
        String actorKind,
        UUID actorId,
        String displayName,
        Instant joinedAt) {}
