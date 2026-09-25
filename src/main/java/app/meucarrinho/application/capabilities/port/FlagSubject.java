package app.meucarrinho.application.capabilities.port;

import app.meucarrinho.application.capabilities.api.ClientContext;
import app.meucarrinho.domain.shared.ActorRef;

public record FlagSubject(ActorRef actor, ClientContext client) {}
