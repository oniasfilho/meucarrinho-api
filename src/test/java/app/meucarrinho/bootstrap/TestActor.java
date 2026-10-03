package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.domain.shared.AccountId;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Stands in for authentication (step 5) in web tests: the test says who is calling. */
public final class TestActor implements CurrentActor {
    private volatile @Nullable AccountId account;

    @Override
    public Optional<AccountId> account() {
        return Optional.ofNullable(account);
    }

    public void signInAs(AccountId id) {
        account = id;
    }

    public void signOut() {
        account = null;
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Configuration {
        @Bean
        @Primary
        TestActor testActor() {
            return new TestActor();
        }
    }
}
