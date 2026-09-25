package app.meucarrinho.domain.shared;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class Text {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private Text() {}

    static String clean(String value, String what, int min, int max) {
        String cleaned = SPACES.matcher(value.strip()).replaceAll(" ");
        int length = cleaned.codePointCount(0, cleaned.length());
        if (length < min || length > max) {
            throw new IllegalArgumentException(what + " must be " + min + "-" + max + " characters: '" + value + "'");
        }
        return cleaned;
    }

    static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return SPACES.matcher(MARKS.matcher(decomposed).replaceAll("")).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }
}
