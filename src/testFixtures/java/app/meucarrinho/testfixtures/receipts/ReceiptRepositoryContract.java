package app.meucarrinho.testfixtures.receipts;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.receipt.ReceiptLine;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.StoreName;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ReceiptRepositoryContract {
    private ReceiptRepository repository;
    private final AccountId marina = TestIds.accountId();
    private final AccountId jessica = TestIds.accountId();

    protected abstract ReceiptRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    private Receipt receipt(String completedAt, AccountId... participants) {
        List<ActorRef> people = java.util.Arrays.stream(participants).map(ActorRef::account).toList();
        return Receipt.of(TestIds.receiptId(), TestIds.listId(), new ListName("Feira de domingo"),
                Optional.of(new StoreName("Feira")), Instant.parse(completedAt), people.getFirst(), people,
                List.of(ReceiptLine.of(new ItemName("banana"), Quantity.kilograms("1.5"), Optional.of(Money.brl(6_99))),
                        ReceiptLine.of(new ItemName("sacola"), Quantity.one(), Optional.empty())),
                Optional.of(Budget.brl(20_00)));
    }

    @Test
    void saves_and_loads_a_receipt_unchanged() {
        Receipt receipt = receipt("2026-09-01T10:00:00Z", marina);

        repository.save(receipt);

        assertThat(repository.findById(receipt.id())).contains(receipt);
    }

    @Test
    void saving_the_same_receipt_twice_is_harmless() {
        Receipt receipt = receipt("2026-09-01T10:00:00Z", marina);

        repository.save(receipt);
        repository.save(receipt);

        assertThat(repository.findVisibleTo(marina, Instant.EPOCH, Instant.parse("2030-01-01T00:00:00Z"))).hasSize(1);
    }

    @Test
    void finds_a_participants_receipts_in_a_half_open_range_newest_first() {
        Receipt august = repository(receipt("2026-08-31T23:59:59Z", marina));
        Receipt early = repository(receipt("2026-09-01T00:00:00Z", marina, jessica));
        Receipt late = repository(receipt("2026-09-30T20:00:00Z", marina));
        repository(receipt("2026-10-01T00:00:00Z", marina));
        repository(receipt("2026-09-15T00:00:00Z", jessica));

        List<Receipt> september = repository.findVisibleTo(marina, Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-10-01T00:00:00Z"));

        assertThat(september).containsExactly(late, early);
        assertThat(september).doesNotContain(august);
    }

    @Test
    void finds_the_latest_receipt_a_participant_can_see() {
        repository(receipt("2026-09-01T10:00:00Z", marina));
        Receipt latest = repository(receipt("2026-09-20T10:00:00Z", jessica, marina));

        assertThat(repository.findLatestVisibleTo(marina)).contains(latest);
        assertThat(repository.findLatestVisibleTo(TestIds.accountId())).isEmpty();
    }

    private Receipt repository(Receipt receipt) {
        repository.save(receipt);
        return receipt;
    }
}
