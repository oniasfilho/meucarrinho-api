package app.meucarrinho.domain.shared;

public record ItemName(String value) {
    public static final int MAX_LENGTH = 120;

    public ItemName {
        value = Text.clean(value, "Item name", 1, MAX_LENGTH);
    }

    public String normalized() {
        return Text.normalize(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
