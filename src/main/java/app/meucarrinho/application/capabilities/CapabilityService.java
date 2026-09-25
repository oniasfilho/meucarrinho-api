package app.meucarrinho.application.capabilities;

import app.meucarrinho.application.capabilities.api.Capability;
import app.meucarrinho.application.capabilities.api.CapabilityPolicy;
import app.meucarrinho.application.capabilities.api.CapabilityUnavailable;
import app.meucarrinho.application.capabilities.api.ClientContext;
import app.meucarrinho.application.capabilities.port.FeatureFlags;
import app.meucarrinho.application.capabilities.port.FlagKey;
import app.meucarrinho.application.capabilities.port.FlagSubject;
import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.telemetry.port.EventEnvelope;
import app.meucarrinho.application.telemetry.port.ProductAnalytics;
import app.meucarrinho.application.telemetry.port.ProductEvent;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ClientPlatform;
import app.meucarrinho.domain.shared.Result;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class CapabilityService implements CapabilityPolicy {
    private final FeatureFlags flags;
    private final ProductAnalytics analytics;
    private final IdGenerator ids;
    private final Clock clock;

    public CapabilityService(FeatureFlags flags, ProductAnalytics analytics, IdGenerator ids, Clock clock) {
        this.flags = flags;
        this.analytics = analytics;
        this.ids = ids;
        this.clock = clock;
    }

    public static boolean defaultFor(Capability capability, ClientPlatform platform) {
        return switch (capability) {
            case OFFLINE_SYNC, GUEST_IMPORT, PHOTOS_BEYOND_BASE_LIMIT -> platform.isMobile();
        };
    }

    public static FlagKey flagKey(Capability capability) {
        return new FlagKey(capability.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    @Override
    public Set<Capability> resolve(ActorRef actor, ClientContext client) {
        return Arrays.stream(Capability.values())
                .filter(capability -> isEnabled(actor, client, capability))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Capability.class)));
    }

    @Override
    public Result<@Nullable Void, CapabilityUnavailable> require(ActorRef actor, ClientContext client,
            Capability capability) {
        if (isEnabled(actor, client, capability)) {
            return Result.ok();
        }
        analytics.track(new ProductEvent.CapabilityDenied(
                new EventEnvelope(ids.next(), clock.now(), Optional.of(actor), Optional.empty(), Optional.empty(),
                        Optional.of(client.platform()), Optional.of(client.version()), Optional.empty(),
                        EventEnvelope.SCHEMA_VERSION),
                capability.name().toLowerCase(Locale.ROOT),
                client.platform().name().toLowerCase(Locale.ROOT),
                actorKind(actor)));
        return Result.err(new CapabilityUnavailable(capability, client.platform()));
    }

    private boolean isEnabled(ActorRef actor, ClientContext client, Capability capability) {
        return flags.isEnabled(flagKey(capability), new FlagSubject(actor, client),
                defaultFor(capability, client.platform()));
    }

    private static String actorKind(ActorRef actor) {
        return switch (actor) {
            case ActorRef.AccountActor a -> "account";
            case ActorRef.GuestActor g -> "guest";
            case ActorRef.DeviceActor d -> "device";
        };
    }
}
