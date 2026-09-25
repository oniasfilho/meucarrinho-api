package app.meucarrinho.domain.shared;

public record StoreName(String value) {
    public static final int MAX_LENGTH = 80;

    public StoreName {
        value = Text.clean(value, "Store name", 1, MAX_LENGTH);
    }

    public String normalized() {
        return Text.normalize(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
