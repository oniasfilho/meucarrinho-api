package app.meucarrinho.domain.shared;

import java.util.Optional;

public sealed interface ActorRef {
    record AccountActor(AccountId id) implements ActorRef {}

    record GuestActor(GuestId id) implements ActorRef {}

    record DeviceActor(InstallId id) implements ActorRef {}

    static ActorRef account(AccountId id) {
        return new AccountActor(id);
    }

    static ActorRef guest(GuestId id) {
        return new GuestActor(id);
    }

    static ActorRef device(InstallId id) {
        return new DeviceActor(id);
    }

    default Optional<AccountId> accountId() {
        return this instanceof AccountActor(AccountId id) ? Optional.of(id) : Optional.empty();
    }
}
