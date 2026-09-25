package app.meucarrinho.application.common.port;

import java.time.Instant;

public interface Clock {
    Instant now();
}
