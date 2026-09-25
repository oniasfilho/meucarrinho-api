package app.meucarrinho.testfixtures.sharing;

import app.meucarrinho.application.sharing.port.GuestPass;
import app.meucarrinho.application.sharing.port.GuestPassError;
import app.meucarrinho.application.sharing.port.GuestPassStore;
import app.meucarrinho.application.sharing.port.GuestPassToken;
import app.meucarrinho.application.sharing.port.IssuedGuestPass;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.testfixtures.common.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class InMemoryGuestPassStore implements GuestPassStore, Transactional {
    private record Entry(GuestPass pass, boolean revoked) {}

    private final Map<String, Entry> byHash = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Override
    public Result<IssuedGuestPass, GuestPassError> issue(ListId list, GuestId guest, DisplayName displayName,
            Instant now) {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        GuestPassToken token = new GuestPassToken(Base64.getUrlEncoder().withoutPadding().encodeToString(secret));
        GuestPass pass = new GuestPass(list, guest, displayName, now.plus(VALIDITY));
        byHash.put(hash(token), new Entry(pass, false));
        return Result.ok(new IssuedGuestPass(token, pass));
    }

    @Override
    public Result<GuestPass, GuestPassError> verify(GuestPassToken token, Instant now) {
        String key = hash(token);
        Entry entry = byHash.get(key);
        if (entry == null) {
            return Result.err(new GuestPassError.Unknown());
        }
        if (entry.revoked()) {
            return Result.err(new GuestPassError.Revoked());
        }
        if (!now.isBefore(entry.pass().expiresAt())) {
            return Result.err(new GuestPassError.Expired());
        }
        GuestPass p = entry.pass();
        GuestPass extended = new GuestPass(p.listId(), p.guestId(), p.displayName(), now.plus(VALIDITY));
        byHash.put(key, new Entry(extended, false));
        return Result.ok(extended);
    }

    @Override
    public Result<@Nullable Void, GuestPassError> revoke(ListId list, GuestId guest) {
        byHash.replaceAll((key, entry) ->
                entry.pass().listId().equals(list) && entry.pass().guestId().equals(guest)
                        ? new Entry(entry.pass(), true)
                        : entry);
        return Result.ok();
    }

    public boolean storesInClear(GuestPassToken token) {
        return byHash.containsKey(token.value());
    }

    private static String hash(GuestPassToken token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.value().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(byHash);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        byHash.clear();
        byHash.putAll((Map<String, Entry>) checkpoint);
    }
}
