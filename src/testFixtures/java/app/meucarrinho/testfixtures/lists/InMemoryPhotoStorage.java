package app.meucarrinho.testfixtures.lists;

import app.meucarrinho.application.lists.port.PhotoContentType;
import app.meucarrinho.application.lists.port.PhotoStorage;
import app.meucarrinho.application.lists.port.PresignedUpload;
import app.meucarrinho.application.lists.port.StorageError;
import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Result;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class InMemoryPhotoStorage implements PhotoStorage {
    private static final Instant EPOCH = Instant.parse("2026-01-01T00:00:00Z");
    private final Set<PhotoRef> uploads = new HashSet<>();

    @Override
    public Result<PresignedUpload, StorageError> presignUpload(PhotoRef ref, PhotoContentType type, long sizeBytes,
            Duration ttl) {
        if (sizeBytes > MAX_BYTES) {
            return Result.err(new StorageError.TooLarge(MAX_BYTES));
        }
        uploads.add(ref);
        return Result.ok(new PresignedUpload(URI.create("memory://photos/" + ref.key() + "?upload"),
                Map.of("Content-Type", type.mediaType()), EPOCH.plus(ttl)));
    }

    @Override
    public Result<URI, StorageError> presignRead(PhotoRef ref, Duration ttl) {
        return uploads.contains(ref)
                ? Result.ok(URI.create("memory://photos/" + ref.key()))
                : Result.err(new StorageError.NotFound());
    }

    @Override
    public Result<@Nullable Void, StorageError> delete(PhotoRef ref) {
        uploads.remove(ref);
        return Result.ok();
    }

    public Set<PhotoRef> stored() {
        return Set.copyOf(uploads);
    }
}
