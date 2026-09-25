package app.meucarrinho.application.common.port;

public abstract sealed class PersistenceException extends RuntimeException
        permits PersistenceException.ConcurrentModification, PersistenceException.StoreUnavailable {
    private static final long serialVersionUID = 1L;

    private PersistenceException(String message) {
        super(message);
    }

    public static final class ConcurrentModification extends PersistenceException {
        private static final long serialVersionUID = 1L;
        private final String aggregateId;
        private final long currentVersion;

        public ConcurrentModification(String aggregateId, long expectedVersion, long currentVersion) {
            super("Aggregate " + aggregateId + " is at version " + currentVersion + ", not " + expectedVersion);
            this.aggregateId = aggregateId;
            this.currentVersion = currentVersion;
        }

        public String aggregateId() {
            return aggregateId;
        }

        public long currentVersion() {
            return currentVersion;
        }
    }

    public static final class StoreUnavailable extends PersistenceException {
        private static final long serialVersionUID = 1L;

        public StoreUnavailable(String reason) {
            super(reason);
        }
    }
}
