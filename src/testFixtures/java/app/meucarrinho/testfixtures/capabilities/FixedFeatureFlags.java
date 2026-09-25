package app.meucarrinho.testfixtures.capabilities;

import app.meucarrinho.application.capabilities.port.FeatureFlags;
import app.meucarrinho.application.capabilities.port.FlagKey;
import app.meucarrinho.application.capabilities.port.FlagSubject;
import java.util.HashMap;
import java.util.Map;

public final class FixedFeatureFlags implements FeatureFlags {
    private final Map<FlagKey, Boolean> flags = new HashMap<>();

    public FixedFeatureFlags set(String key, boolean enabled) {
        flags.put(new FlagKey(key), enabled);
        return this;
    }

    public FixedFeatureFlags unset(String key) {
        flags.remove(new FlagKey(key));
        return this;
    }

    @Override
    public boolean isEnabled(FlagKey key, FlagSubject subject, boolean fallback) {
        return flags.getOrDefault(key, fallback);
    }
}
