package app.meucarrinho.domain.shared;

import java.util.regex.Pattern;

public record AppVersion(String value) {
    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+([-+][0-9A-Za-z.-]+)?$");

    public AppVersion {
        if (!SEMVER.matcher(value).matches()) {
            throw new IllegalArgumentException("Not an app version: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
