package app.meucarrinho.domain.shared;

public record ListName(String value) {
    public static final int MAX_LENGTH = 80;

    public ListName {
        value = Text.clean(value, "List name", 1, MAX_LENGTH);
    }

    @Override
    public String toString() {
        return value;
    }
}
