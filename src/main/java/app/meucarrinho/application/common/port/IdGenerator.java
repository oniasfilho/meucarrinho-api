package app.meucarrinho.application.common.port;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.util.UUID;

public interface IdGenerator {
    UUID next();

    default ListId newListId() {
        return new ListId(next());
    }

    default ItemId newItemId() {
        return new ItemId(next());
    }

    default ReceiptId newReceiptId() {
        return new ReceiptId(next());
    }

    default AccountId newAccountId() {
        return new AccountId(next());
    }

    default GuestId newGuestId() {
        return new GuestId(next());
    }
}
