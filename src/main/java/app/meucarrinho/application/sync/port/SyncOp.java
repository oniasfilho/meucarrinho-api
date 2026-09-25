package app.meucarrinho.application.sync.port;

import app.meucarrinho.domain.shared.ItemId;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record SyncOp(
        ClientOpId clientOpId,
        long baseSeq,
        OpType op,
        Optional<ItemId> itemId,
        Map<String, @Nullable Object> fields,
        OffsetDateTime clientAt) {
    public SyncOp {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
}
