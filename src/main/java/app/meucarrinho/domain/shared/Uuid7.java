package app.meucarrinho.domain.shared;

import java.util.UUID;
import java.util.random.RandomGenerator;

public final class Uuid7 {
    private Uuid7() {}

    public static UUID generate(long epochMillis, RandomGenerator random) {
        if (epochMillis < 0 || epochMillis >= (1L << 48)) {
            throw new IllegalArgumentException("Timestamp out of UUIDv7 range: " + epochMillis);
        }
        long randA = random.nextLong() & 0xFFFL;
        long msb = (epochMillis << 16) | (0x7L << 12) | randA;
        long lsb = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }

    public static boolean isV7(UUID uuid) {
        return uuid.version() == 7 && uuid.variant() == 2;
    }

    static UUID require(UUID uuid, String what) {
        if (!isV7(uuid)) {
            throw new IllegalArgumentException(what + " must be a UUIDv7: " + uuid);
        }
        return uuid;
    }
}
