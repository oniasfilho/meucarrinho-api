package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.InstallId;
import java.util.UUID;

final class ActorRefMapper {
    private ActorRefMapper() {}

    static String kind(ActorRef actor) {
        return switch (actor) {
            case ActorRef.AccountActor ignored -> "ACCOUNT";
            case ActorRef.GuestActor ignored -> "GUEST";
            case ActorRef.DeviceActor ignored -> "DEVICE";
        };
    }

    static UUID id(ActorRef actor) {
        return switch (actor) {
            case ActorRef.AccountActor account -> account.id().value();
            case ActorRef.GuestActor guest -> guest.id().value();
            case ActorRef.DeviceActor device -> device.id().value();
        };
    }

    static ActorRef from(String kind, UUID id) {
        return switch (kind) {
            case "ACCOUNT" -> ActorRef.account(new AccountId(id));
            case "GUEST" -> ActorRef.guest(new GuestId(id));
            case "DEVICE" -> ActorRef.device(new InstallId(id));
            default -> throw new IllegalArgumentException("Unknown actor kind " + kind);
        };
    }
}
