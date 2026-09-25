package app.meucarrinho.application.accounts.port;

import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import java.util.Optional;

public record IdentityProfile(ExternalRef subject, DisplayName displayName, Optional<EmailAddress> email,
        boolean emailVerified) {}
