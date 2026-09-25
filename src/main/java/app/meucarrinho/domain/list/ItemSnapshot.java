package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ItemNote;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Quantity;
import java.time.Instant;
import java.util.Optional;

public record ItemSnapshot(
        ItemId id,
        ItemName name,
        Quantity quantity,
        Optional<Money> unitPrice,
        Optional<ItemNote> note,
        Optional<PhotoRef> photoRef,
        boolean picked,
        Optional<ActorRef> pickedBy,
        ActorRef lastEditedBy,
        int position,
        Optional<Instant> removedAt) {}
