package app.meucarrinho.application.capabilities.port;

public interface FeatureFlags {
    boolean isEnabled(FlagKey key, FlagSubject subject, boolean fallback);
}
