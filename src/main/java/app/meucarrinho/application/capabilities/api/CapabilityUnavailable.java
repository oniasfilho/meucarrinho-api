package app.meucarrinho.application.capabilities.api;

import app.meucarrinho.domain.shared.ClientPlatform;

public record CapabilityUnavailable(Capability capability, ClientPlatform platform) {}
