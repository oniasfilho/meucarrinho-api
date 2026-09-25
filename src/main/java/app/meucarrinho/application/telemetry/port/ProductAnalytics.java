package app.meucarrinho.application.telemetry.port;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.InstallId;

public interface ProductAnalytics {
    void track(ProductEvent event);

    void identify(InstallId install, AccountId account);
}
