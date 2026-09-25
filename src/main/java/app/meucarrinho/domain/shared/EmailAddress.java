package app.meucarrinho.domain.shared;

import java.util.Locale;
import java.util.regex.Pattern;

public record EmailAddress(String value) {
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > 254 || !SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Not an e-mail address: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
