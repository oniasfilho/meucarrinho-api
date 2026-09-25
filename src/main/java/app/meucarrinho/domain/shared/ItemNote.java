package app.meucarrinho.domain.shared;

public record ItemNote(String value) {
    public static final int MAX_LENGTH = 280;

    public ItemNote {
        value = Text.clean(value, "Note", 1, MAX_LENGTH);
    }

    @Override
    public String toString() {
        return value;
    }
}
