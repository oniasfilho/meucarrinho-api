package app.meucarrinho.application.telemetry.port;

public sealed interface ProductEvent {
    EventEnvelope envelope();

    enum JoinedAs {
        ACCOUNT,
        GUEST
    }

    record InviteAccepted(EventEnvelope envelope, String channel, long minutesToAccept, JoinedAs joinedAs)
            implements ProductEvent {}

    record GuestPassClaimed(EventEnvelope envelope, long daysAsGuest, int editsAsGuest) implements ProductEvent {}

    record GuestImportCompleted(EventEnvelope envelope, int lists, int receipts, int ops) implements ProductEvent {}

    record SyncCompleted(EventEnvelope envelope, int ops, long offlineMinutes, int conflicts, int rejections)
            implements ProductEvent {}

    record CapabilityDenied(EventEnvelope envelope, String capability, String platform, String actorKind)
            implements ProductEvent {}

    record CheckoutStarted(EventEnvelope envelope, String plan, String provider) implements ProductEvent {}

    record SubscriptionActivated(EventEnvelope envelope, String plan, String provider) implements ProductEvent {}
}
