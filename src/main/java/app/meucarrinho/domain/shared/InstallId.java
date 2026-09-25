package app.meucarrinho.domain.shared;

import java.util.UUID;

public record InstallId(UUID value) {
    public static InstallId of(String value) {
        return new InstallId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
