package app.meucarrinho.application.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.capabilities.api.Capability;
import app.meucarrinho.application.capabilities.api.CapabilityUnavailable;
import app.meucarrinho.application.capabilities.api.ClientContext;
import app.meucarrinho.application.telemetry.port.ProductEvent;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.AppVersion;
import app.meucarrinho.domain.shared.ClientPlatform;
import app.meucarrinho.domain.shared.InstallId;
import app.meucarrinho.testfixtures.InMemoryCore;
import app.meucarrinho.testfixtures.TestIds;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CapabilityServiceTest {
    private final InMemoryCore core = new InMemoryCore();
    private final ActorRef marina = ActorRef.account(TestIds.accountId());

    private static ClientContext on(ClientPlatform platform) {
        return new ClientContext(platform, new AppVersion("2.3.0"), new InstallId(UUID.randomUUID()));
    }

    @Test
    void mobile_gets_everything_and_web_nothing_by_default() {
        assertThat(core.capabilities.resolve(marina, on(ClientPlatform.IOS))).isEqualTo(EnumSet.allOf(Capability.class));
        assertThat(core.capabilities.resolve(marina, on(ClientPlatform.ANDROID))).isEqualTo(EnumSet.allOf(Capability.class));
        assertThat(core.capabilities.resolve(marina, on(ClientPlatform.WEB))).isEmpty();
    }

    @Test
    void flags_override_the_defaults() {
        core.flags.set("offline-sync", true).set("guest-import", false);

        assertThat(core.capabilities.resolve(marina, on(ClientPlatform.WEB))).containsExactly(Capability.OFFLINE_SYNC);
        assertThat(core.capabilities.resolve(marina, on(ClientPlatform.IOS)))
                .containsExactlyInAnyOrder(Capability.OFFLINE_SYNC, Capability.PHOTOS_BEYOND_BASE_LIMIT);
    }

    @Test
    void a_denied_capability_is_reported_to_product_analytics() {
        var result = core.capabilities.require(marina, on(ClientPlatform.WEB), Capability.OFFLINE_SYNC);

        assertThat(result.errorOrThrow()).isEqualTo(new CapabilityUnavailable(Capability.OFFLINE_SYNC, ClientPlatform.WEB));
        assertThat(core.analytics.events(ProductEvent.CapabilityDenied.class)).singleElement().satisfies(event -> {
            assertThat(event.capability()).isEqualTo("offline_sync");
            assertThat(event.platform()).isEqualTo("web");
            assertThat(event.actorKind()).isEqualTo("account");
        });
    }

    @Test
    void an_allowed_capability_emits_nothing() {
        assertThat(core.capabilities.require(marina, on(ClientPlatform.ANDROID), Capability.GUEST_IMPORT).isOk()).isTrue();
        assertThat(core.analytics.events()).isEmpty();
    }
}
