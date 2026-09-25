package app.meucarrinho.application.billing.port;

import app.meucarrinho.domain.shared.ExternalRef;
import java.net.URI;

public record CheckoutSession(URI url, ExternalRef ref) {}
