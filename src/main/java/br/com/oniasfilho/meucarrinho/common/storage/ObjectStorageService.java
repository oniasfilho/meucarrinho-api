package br.com.oniasfilho.meucarrinho.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Thin wrapper over S3 for label-photo objects. Concrete on purpose: the integration
 * test exercises a real (LocalStack) bucket, so there is no second implementation to
 * justify an interface.
 */
@Service
public class ObjectStorageService {

    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration presignTtl;

    public ObjectStorageService(S3Client s3Client, S3Presigner s3Presigner, S3StorageProperties props) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = props.bucket();
        this.presignTtl = props.presignTtl();
    }

    /**
     * Stores {@code file} under {@code keyPrefix} with a generated, collision-free key.
     *
     * @throws IllegalArgumentException when the file is empty, too large or not an allowed image type
     */
    public StoredObject store(String keyPrefix, MultipartFile file) {
        validate(file);
        String key = keyPrefix + "/" + UUID.randomUUID() + extension(file.getContentType());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file.", e);
        }
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromBytes(bytes));
        return new StoredObject(key, bytes.length, file.getContentType());
    }

    /** Empty when {@code key} is null/blank; otherwise a short-lived presigned GET URL. */
    public Optional<PresignedUrl> presignedGet(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
            .signatureDuration(presignTtl)
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build());
        return Optional.of(new PresignedUrl(presigned.url().toString(), presigned.expiration()));
    }

    /** Best-effort delete; a missing object is not an error. */
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (NoSuchKeyException ignored) {
            // already gone
        }
    }

    public byte[] getBytes(String key) {
        return s3Client.getObjectAsBytes(b -> b.bucket(bucket).key(key)).asByteArray();
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Uploaded file exceeds the 5 MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Unsupported image type. Allowed: image/jpeg, image/png, image/webp.");
        }
    }

    private static String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    public record StoredObject(String key, long size, String contentType) {
    }

    public record PresignedUrl(String url, Instant expiresAt) {
    }
}
