package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.shared.Uuid7;
import java.time.Instant;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SystemClockAndIdsTest {
    @Test
    void the_system_clock_reads_the_wall_clock() {
        Instant before = Instant.now();
        Instant now = new SystemClock().now();

        assertThat(now).isBetween(before, Instant.now());
    }

    @Test
    void ids_are_unique_uuidv7_stamped_with_the_clock() {
        Instant now = Instant.parse("2026-10-03T12:00:00Z");
        Uuid7IdGenerator ids = new Uuid7IdGenerator(() -> now, new SplittableRandom(7));

        Set<UUID> minted = IntStream.range(0, 10_000).mapToObj(i -> ids.next()).collect(Collectors.toSet());

        assertThat(minted).hasSize(10_000).allMatch(Uuid7::isV7)
                .allMatch(id -> id.getMostSignificantBits() >>> 16 == now.toEpochMilli());
    }
}
