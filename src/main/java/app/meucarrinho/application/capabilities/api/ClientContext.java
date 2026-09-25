package app.meucarrinho.application.capabilities.api;

import app.meucarrinho.domain.shared.AppVersion;
import app.meucarrinho.domain.shared.ClientPlatform;
import app.meucarrinho.domain.shared.InstallId;

public record ClientContext(ClientPlatform platform, AppVersion version, InstallId install) {}
