package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.ActorDto;
import java.time.Instant;

public record MemberResponse(ActorDto actor, String displayName, Instant joinedAt) {}
