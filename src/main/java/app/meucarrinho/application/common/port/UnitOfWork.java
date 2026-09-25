package app.meucarrinho.application.common.port;

import java.util.function.Supplier;

public interface UnitOfWork {
    <T> T execute(Supplier<T> work);
}
