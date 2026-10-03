package app.meucarrinho.application.common.port;

import app.meucarrinho.domain.shared.AccountId;
import java.util.Objects;
import java.util.regex.Pattern;

/** A client's {@code Idempotency-Key}, scoped to the account that sent it (ADR 0010). */
public record IdempotencyKey(AccountId account, String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    public IdempotencyKey {
        Objects.requireNonNull(account, "account");
        if (!isWellFormed(value)) {
            throw new IllegalArgumentException("An idempotency key is 1 to 128 characters of A-Z a-z 0-9 . _ : -");
        }
    }

    public static boolean isWellFormed(String value) {
        return FORMAT.matcher(value).matches();
    }
}
