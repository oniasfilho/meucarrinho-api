package app.meucarrinho.domain.shared;

import java.util.regex.Pattern;

public record PhotoRef(String key) {
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._/-]{1,512}$");

    public PhotoRef {
        if (!SAFE.matcher(key).matches() || key.contains("..")) {
            throw new IllegalArgumentException("Invalid photo key: " + key);
        }
    }
}
