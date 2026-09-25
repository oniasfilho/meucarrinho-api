package app.meucarrinho.application.billing.port;

import java.net.URI;

public record ReturnUrls(URI success, URI cancel) {}
