package app.meucarrinho.testfixtures.sync;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.sync.port.ChangeLog;
import app.meucarrinho.application.sync.port.ChangeLogError;
import app.meucarrinho.application.sync.port.ClientOpId;
import app.meucarrinho.application.sync.port.LoggedOp;
import app.meucarrinho.application.sync.port.OpType;
import app.meucarrinho.application.sync.port.SyncOp;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ChangeLogContract {
    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");
    private ChangeLog log;
    private final ActorRef marina = ActorRef.account(TestIds.accountId());

    protected abstract ChangeLog createChangeLog();

    @BeforeEach
    void setUp() {
        log = createChangeLog();
    }

    private SyncOp op(OpType type) {
        return new SyncOp(new ClientOpId(TestIds.uuid()), 0, type, Optional.of(TestIds.itemId()),
                Map.of("unitPrice", 890), OffsetDateTime.of(2026, 9, 24, 18, 41, 7, 0, ZoneOffset.ofHours(-4)));
    }

    private LoggedOp append(ListId list, SyncOp op, Instant at) {
        return log.append(list, op, marina, at).orElseThrow();
    }

    @Test
    void numbers_ops_from_one_per_list_without_gaps() {
        ListId a = TestIds.listId(), b = TestIds.listId();

        assertThat(append(a, op(OpType.ITEM_ADD), T0).seq()).isEqualTo(1);
        assertThat(append(a, op(OpType.ITEM_PICK), T0).seq()).isEqualTo(2);
        assertThat(append(b, op(OpType.ITEM_ADD), T0).seq()).isEqualTo(1);
        assertThat(log.latestSeq(a)).isEqualTo(2);
        assertThat(log.latestSeq(TestIds.listId())).isZero();
    }

    @Test
    void keeps_what_was_appended() {
        ListId list = TestIds.listId();
        SyncOp op = op(OpType.ITEM_EDIT);

        LoggedOp logged = append(list, op, T0);

        assertThat(logged).isEqualTo(new LoggedOp(list, 1, op, marina, T0));
        assertThat(log.since(list, 0).orElseThrow()).containsExactly(logged);
    }

    @Test
    void applies_each_client_op_once_per_list() {
        ListId list = TestIds.listId();
        SyncOp op = op(OpType.ITEM_PICK);
        LoggedOp first = append(list, op, T0);

        assertThat(log.append(list, op, marina, T0.plusSeconds(5)).errorOrThrow())
                .isEqualTo(new ChangeLogError.DuplicateOp(first));
        assertThat(log.latestSeq(list)).isEqualTo(1);
        assertThat(log.append(TestIds.listId(), op, marina, T0).isOk()).as("other lists are independent").isTrue();
    }

    @Test
    void returns_ops_after_a_sequence_number_in_order() {
        ListId list = TestIds.listId();
        append(list, op(OpType.ITEM_ADD), T0);
        LoggedOp second = append(list, op(OpType.ITEM_EDIT), T0);
        LoggedOp third = append(list, op(OpType.ITEM_PICK), T0);

        assertThat(log.since(list, 1).orElseThrow()).containsExactly(second, third);
        assertThat(log.since(list, 3).orElseThrow()).isEmpty();
    }

    @Test
    void tells_a_client_that_fell_behind_the_retention_window() {
        ListId list = TestIds.listId();
        append(list, op(OpType.ITEM_ADD), T0);
        append(list, op(OpType.ITEM_EDIT), T0);
        LoggedOp recent = append(list, op(OpType.ITEM_PICK), T0.plus(Duration.ofDays(31)));

        log.purgeAppliedBefore(T0.plus(Duration.ofDays(1)));

        assertThat(log.since(list, 1).errorOrThrow()).isEqualTo(new ChangeLogError.ChangesExpired(3));
        assertThat(log.since(list, 2).orElseThrow()).containsExactly(recent);
        assertThat(append(list, op(OpType.ITEM_UNPICK), T0.plus(Duration.ofDays(31))).seq())
                .as("sequence numbers are never reused").isEqualTo(4);
    }
}
