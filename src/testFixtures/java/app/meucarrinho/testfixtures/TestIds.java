package app.meucarrinho.testfixtures;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Uuid7;
import java.util.SplittableRandom;
import java.util.UUID;

public final class TestIds {
    private static final long EPOCH = 1_790_000_000_000L;
    private static final SplittableRandom RANDOM = new SplittableRandom(42);
    private static long counter;

    private TestIds() {}

    public static synchronized UUID uuid() {
        return Uuid7.generate(EPOCH + counter++, RANDOM);
    }

    public static ListId listId() {
        return new ListId(uuid());
    }

    public static ItemId itemId() {
        return new ItemId(uuid());
    }

    public static ReceiptId receiptId() {
        return new ReceiptId(uuid());
    }

    public static AccountId accountId() {
        return new AccountId(uuid());
    }

    public static GuestId guestId() {
        return new GuestId(uuid());
    }
}
