package app.meucarrinho.domain.shared;

public record DisplayName(String value) {
    public DisplayName {
        value = Text.clean(value, "Display name", 1, 60);
    }

    public String firstName() {
        int space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space);
    }

    @Override
    public String toString() {
        return value;
    }
}
