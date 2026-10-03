package app.meucarrinho.api.rest;

import app.meucarrinho.application.accounts.UpsertAccount;
import app.meucarrinho.bootstrap.InMemoryApplicationTest;
import app.meucarrinho.bootstrap.TestActor;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ExternalRef;
import jakarta.servlet.Filter;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Web tests: real controllers, filters, use cases and fakes, no server and no Docker. {@link #client} builds the typed Java
 * client from the same API interfaces the controllers implement (spec §3); {@link #http} sends raw requests when a
 * test needs to see status codes and problem bodies.
 */
@InMemoryApplicationTest
public abstract class RestTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UpsertAccount accounts;

    @Autowired
    protected TestActor actor;

    protected MockMvcTester http;

    private HttpServiceProxyFactory clients;

    @BeforeEach
    void connect() {
        Filter[] filters = context.getBeansOfType(Filter.class).values().stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toArray(Filter[]::new);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
        http = MockMvcTester.create(mvc);
        RestClient rest = RestClient.builder()
                .requestFactory(new MockMvcClientHttpRequestFactory(mvc))
                .baseUrl("http://localhost")
                .build();
        clients = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest)).build();
        actor.signOut();
    }

    protected <T> T client(Class<T> api) {
        return clients.createClient(api);
    }

    /** Provisions a fresh account, as PUT /v1/me will in step 5, and makes it the caller. */
    protected AccountId signIn(String name) {
        AccountId id = accounts.upsert(new ExternalRef("test", name + "|" + UUID.randomUUID()), new DisplayName(name),
                Optional.empty()).orElseThrow().id();
        actor.signInAs(id);
        return id;
    }
}
