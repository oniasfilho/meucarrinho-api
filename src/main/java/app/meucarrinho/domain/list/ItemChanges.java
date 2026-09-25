package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.Change;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ItemNote;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Quantity;
import java.util.Optional;

public record ItemChanges(
        Optional<ItemName> name,
        Optional<Quantity> quantity,
        Change<Money> unitPrice,
        Change<ItemNote> note,
        Change<PhotoRef> photoRef) {
    public ItemChanges {
        if (unitPrice instanceof Change.Set<Money>(Money price)) {
            ItemDraft.requireValidPrice(price);
        }
    }

    public static ItemChanges none() {
        return new ItemChanges(Optional.empty(), Optional.empty(), Change.keep(), Change.keep(), Change.keep());
    }

    public ItemChanges withName(ItemName value) {
        return new ItemChanges(Optional.of(value), quantity, unitPrice, note, photoRef);
    }

    public ItemChanges withQuantity(Quantity value) {
        return new ItemChanges(name, Optional.of(value), unitPrice, note, photoRef);
    }

    public ItemChanges withUnitPrice(Change<Money> value) {
        return new ItemChanges(name, quantity, value, note, photoRef);
    }

    public ItemChanges withNote(Change<ItemNote> value) {
        return new ItemChanges(name, quantity, unitPrice, value, photoRef);
    }

    public ItemChanges withPhotoRef(Change<PhotoRef> value) {
        return new ItemChanges(name, quantity, unitPrice, note, value);
    }
}
