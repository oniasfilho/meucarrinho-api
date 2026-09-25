package app.meucarrinho.domain.shared;

import java.util.Locale;
import java.util.random.RandomGenerator;

public record ShareCode(String value) {
    public static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    public static final int LENGTH = 4;

    public ShareCode {
        if (value.length() != LENGTH || value.chars().anyMatch(c -> ALPHABET.indexOf(c) < 0)) {
            throw new IllegalArgumentException("Not a share code: " + value);
        }
    }

    public static ShareCode parse(String input) {
        String canonical = input.strip()
                .toUpperCase(Locale.ROOT)
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
        return new ShareCode(canonical);
    }

    public static ShareCode random(RandomGenerator random) {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new ShareCode(code.toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
