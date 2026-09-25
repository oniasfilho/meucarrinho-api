package app.meucarrinho.application.lists.port;

import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Result;
import java.net.URI;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

public interface PhotoStorage {
    long MAX_BYTES = 5L * 1024 * 1024;

    Result<PresignedUpload, StorageError> presignUpload(PhotoRef ref, PhotoContentType type, long sizeBytes, Duration ttl);

    Result<URI, StorageError> presignRead(PhotoRef ref, Duration ttl);

    Result<@Nullable Void, StorageError> delete(PhotoRef ref);
}
