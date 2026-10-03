package app.meucarrinho.api.rest;

import app.meucarrinho.domain.shared.ActorRef;
import java.util.UUID;

/** Who made a change: an account, an invited guest or a device during guest import (spec §4). */
public record ActorDto(Type type, UUID id) {
    public enum Type {
        ACCOUNT,
        GUEST,
        DEVICE
    }

    public static ActorDto of(ActorRef actor) {
        return switch (actor) {
            case ActorRef.AccountActor(var id) -> new ActorDto(Type.ACCOUNT, id.value());
            case ActorRef.GuestActor(var id) -> new ActorDto(Type.GUEST, id.value());
            case ActorRef.DeviceActor(var id) -> new ActorDto(Type.DEVICE, id.value());
        };
    }
}
