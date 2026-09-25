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

public final class Item {
    private final ItemId id;
    private ItemName name;
    private Quantity quantity;
    private Optional<Money> unitPrice;
    private Optional<ItemNote> note;
    private Optional<PhotoRef> photoRef;
    private boolean picked;
    private Optional<ActorRef> pickedBy;
    private ActorRef lastEditedBy;
    private int position;
    private Optional<Instant> removedAt;

    Item(ItemId id, ItemDraft draft, ActorRef addedBy, int position) {
        this(new ItemSnapshot(id, draft.name(), draft.quantity(), draft.unitPrice(), draft.note(),
                draft.photoRef(), false, Optional.empty(), addedBy, position, Optional.empty()));
    }

    Item(ItemSnapshot s) {
        this.id = s.id();
        this.name = s.name();
        this.quantity = s.quantity();
        this.unitPrice = s.unitPrice();
        this.note = s.note();
        this.photoRef = s.photoRef();
        this.picked = s.picked();
        this.pickedBy = s.pickedBy();
        this.lastEditedBy = s.lastEditedBy();
        this.position = s.position();
        this.removedAt = s.removedAt();
    }

    public ItemId id() {
        return id;
    }

    public ItemName name() {
        return name;
    }

    public Quantity quantity() {
        return quantity;
    }

    public Optional<Money> unitPrice() {
        return unitPrice;
    }

    public Optional<ItemNote> note() {
        return note;
    }

    public Optional<PhotoRef> photoRef() {
        return photoRef;
    }

    public boolean picked() {
        return picked;
    }

    public Optional<ActorRef> pickedBy() {
        return pickedBy;
    }

    public ActorRef lastEditedBy() {
        return lastEditedBy;
    }

    public int position() {
        return position;
    }

    public Optional<Instant> removedAt() {
        return removedAt;
    }

    public boolean isRemoved() {
        return removedAt.isPresent();
    }

    public Optional<Money> subtotal() {
        return unitPrice.map(price -> price.times(quantity));
    }

    ItemDraft toDraft() {
        return new ItemDraft(name, quantity, unitPrice, note, photoRef);
    }

    ItemSnapshot snapshot() {
        return new ItemSnapshot(id, name, quantity, unitPrice, note, photoRef, picked, pickedBy,
                lastEditedBy, position, removedAt);
    }

    java.util.Set<String> apply(ItemChanges changes, ActorRef editor) {
        var changed = new java.util.LinkedHashSet<String>();
        if (changes.name().isPresent() && !changes.name().get().equals(name)) {
            name = changes.name().get();
            changed.add("name");
        }
        if (changes.quantity().isPresent() && !changes.quantity().get().equals(quantity)) {
            quantity = changes.quantity().get();
            changed.add("quantity");
        }
        Optional<Money> newPrice = changes.unitPrice().applyTo(unitPrice);
        if (!newPrice.equals(unitPrice)) {
            unitPrice = newPrice;
            changed.add("unitPrice");
        }
        Optional<ItemNote> newNote = changes.note().applyTo(note);
        if (!newNote.equals(note)) {
            note = newNote;
            changed.add("note");
        }
        Optional<PhotoRef> newPhoto = changes.photoRef().applyTo(photoRef);
        if (!newPhoto.equals(photoRef)) {
            photoRef = newPhoto;
            changed.add("photoRef");
        }
        if (!changed.isEmpty()) {
            lastEditedBy = editor;
        }
        return changed;
    }

    void pick(ActorRef actor) {
        picked = true;
        pickedBy = Optional.of(actor);
    }

    void unpick() {
        picked = false;
        pickedBy = Optional.empty();
    }

    void remove(Instant at) {
        removedAt = Optional.of(at);
    }

    void restore() {
        removedAt = Optional.empty();
    }
}
