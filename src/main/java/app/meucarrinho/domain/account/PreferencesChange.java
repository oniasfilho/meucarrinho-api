package app.meucarrinho.domain.account;

import java.util.Optional;

public record PreferencesChange(
        Optional<SortOrder> sortOrder,
        Optional<Boolean> collaborationAlerts,
        Optional<Boolean> haptics,
        Optional<Boolean> analyticsOptOut) {
    public static PreferencesChange none() {
        return new PreferencesChange(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public PreferencesChange withSortOrder(SortOrder value) {
        return new PreferencesChange(Optional.of(value), collaborationAlerts, haptics, analyticsOptOut);
    }

    public PreferencesChange withCollaborationAlerts(boolean value) {
        return new PreferencesChange(sortOrder, Optional.of(value), haptics, analyticsOptOut);
    }

    public PreferencesChange withHaptics(boolean value) {
        return new PreferencesChange(sortOrder, collaborationAlerts, Optional.of(value), analyticsOptOut);
    }

    public PreferencesChange withAnalyticsOptOut(boolean value) {
        return new PreferencesChange(sortOrder, collaborationAlerts, haptics, Optional.of(value));
    }
}
