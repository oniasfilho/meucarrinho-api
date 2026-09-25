package app.meucarrinho.application.sharing.port;

import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.ListId;
import java.time.Instant;

public record GuestPass(ListId listId, GuestId guestId, DisplayName displayName, Instant expiresAt) {}
