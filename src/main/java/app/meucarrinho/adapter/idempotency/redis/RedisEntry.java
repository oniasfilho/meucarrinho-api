package app.meucarrinho.adapter.idempotency.redis;

import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The value stored under a key: a version byte, the request fingerprint and, once completed, the response. A running
 * claim encodes to the same bytes every time, which lets the release script compare them.
 */
record RedisEntry(RequestFingerprint request, @Nullable StoredResponse response) {
    private static final byte RUNNING = 1;
    private static final byte COMPLETED = 2;

    static byte[] running(RequestFingerprint request) {
        return new RedisEntry(request, null).encode();
    }

    static byte[] completed(RequestFingerprint request, StoredResponse response) {
        return new RedisEntry(request, response).encode();
    }

    IdempotencyClaim claimFor(RequestFingerprint other) {
        if (!request.equals(other)) {
            return new IdempotencyClaim.KeyReused();
        }
        return response == null ? new IdempotencyClaim.InProgress() : new IdempotencyClaim.Completed(response);
    }

    byte[] encode() {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(response == null ? RUNNING : COMPLETED);
            out.writeUTF(request.value());
            if (response != null) {
                out.writeShort(response.status());
                out.writeShort(response.headers().size());
                for (Map.Entry<String, String> header : response.headers().entrySet()) {
                    out.writeUTF(header.getKey());
                    out.writeUTF(header.getValue());
                }
                out.write(response.body());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    static RedisEntry decode(byte[] stored) {
        try (var in = new DataInputStream(new ByteArrayInputStream(stored))) {
            byte kind = in.readByte();
            RequestFingerprint request = new RequestFingerprint(in.readUTF());
            if (kind == RUNNING) {
                return new RedisEntry(request, null);
            }
            if (kind != COMPLETED) {
                throw new IllegalStateException("Unknown idempotency entry kind " + kind);
            }
            int status = in.readUnsignedShort();
            int count = in.readUnsignedShort();
            Map<String, String> headers = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                headers.put(in.readUTF(), in.readUTF());
            }
            return new RedisEntry(request, new StoredResponse(status, headers, in.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
