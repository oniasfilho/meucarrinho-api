package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.domain.shared.Unit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuickAddParser {
    public record Rejection(String code) {
        public static final Rejection EMPTY = new Rejection("EMPTY");
        public static final Rejection INVALID_QUANTITY = new Rejection("INVALID_QUANTITY");
        public static final Rejection NAME_TOO_LONG = new Rejection("NAME_TOO_LONG");
    }

    private static final Pattern QUANTITY = Pattern.compile("^(\\d{1,4}(?:[.,]\\d{1,3})?)(kg|un|x)?$");
    private static final Pattern PRICE = Pattern.compile("^(r\\$)?(\\d{1,7})(?:[.,](\\d{1,2}))?$");

    private QuickAddParser() {}

    public static Result<ItemDraft, Rejection> parse(String text) {
        List<String> tokens = new ArrayList<>(Arrays.asList(text.strip().split("\\s+")));
        tokens.removeIf(String::isEmpty);
        if (tokens.isEmpty()) {
            return Result.err(Rejection.EMPTY);
        }

        Optional<Money> price = takePrice(tokens);

        BigDecimal amount = BigDecimal.ONE;
        Unit unit = Unit.UN;
        Matcher quantity = QUANTITY.matcher(tokens.getFirst().toLowerCase(Locale.ROOT));
        if (tokens.size() > 1 && quantity.matches()) {
            amount = decimal(quantity.group(1));
            String suffix = quantity.group(2);
            tokens.removeFirst();
            if (suffix == null && tokens.size() > 1 && isUnitWord(tokens.getFirst())) {
                suffix = tokens.removeFirst().toLowerCase(Locale.ROOT);
            }
            unit = "kg".equals(suffix) ? Unit.KG : Unit.UN;
        }
        if (!Quantity.isValid(amount, unit)) {
            return Result.err(Rejection.INVALID_QUANTITY);
        }

        String name = String.join(" ", tokens);
        if (name.codePointCount(0, name.length()) > ItemName.MAX_LENGTH) {
            return Result.err(Rejection.NAME_TOO_LONG);
        }
        return Result.ok(ItemDraft.of(new ItemName(name), new Quantity(amount, unit), price));
    }

    private static Optional<Money> takePrice(List<String> tokens) {
        if (tokens.size() < 2) {
            return Optional.empty();
        }
        Matcher m = PRICE.matcher(tokens.getLast().toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return Optional.empty();
        }
        boolean currencyBefore = tokens.size() > 2 && tokens.get(tokens.size() - 2).equalsIgnoreCase("R$");
        boolean isPrice = m.group(1) != null || m.group(3) != null || currencyBefore;
        if (!isPrice) {
            return Optional.empty();
        }
        tokens.removeLast();
        if (currencyBefore) {
            tokens.removeLast();
        }
        String cents = m.group(3) == null ? "00" : (m.group(3) + "0").substring(0, 2);
        return Optional.of(Money.brl(Long.parseLong(m.group(2)) * 100 + Long.parseLong(cents)));
    }

    private static boolean isUnitWord(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        return t.equals("kg") || t.equals("un");
    }

    private static BigDecimal decimal(String number) {
        return new BigDecimal(number.replace(',', '.'));
    }
}
