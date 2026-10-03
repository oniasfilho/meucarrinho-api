package app.meucarrinho.application.common.port;

/** What {@link IdempotencyStore#claim} found for a key. */
public sealed interface IdempotencyClaim {
    /** The key was free and now belongs to this request until it completes, is released or the lease ends. */
    record Acquired() implements IdempotencyClaim {}

    /** The same request holds the key and has not finished. */
    record InProgress() implements IdempotencyClaim {}

    /** The same request already finished with this response. */
    record Completed(StoredResponse response) implements IdempotencyClaim {}

    /** A different request holds the key, running or finished. */
    record KeyReused() implements IdempotencyClaim {}
}
