package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Stands in for authentication (step 5) in web tests: the test says who is calling. */
public final class TestActor implements CurrentActor {
    private volatile @Nullable ExternalRef identity;
    private volatile @Nullable AccountId account;

    @Override
    public Optional<ExternalRef> identity() {
        return Optional.ofNullable(identity);
    }

    @Override
    public Optional<AccountId> account() {
        return Optional.ofNullable(account);
    }

    /** A verified identity with this account, as a token for a signed-up user would give. */
    public void signInAs(ExternalRef identity, AccountId id) {
        this.identity = identity;
        this.account = id;
    }

    /** An account with no identity the test cares about. */
    public void signInAs(AccountId id) {
        signInAs(new ExternalRef("test", "account|" + id), id);
    }

    /** A verified identity that has no account yet: what a first sign-in looks like before PUT /v1/me. */
    public void signInWithoutAccount(ExternalRef identity) {
        this.identity = identity;
        this.account = null;
    }

    public void signOut() {
        identity = null;
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
