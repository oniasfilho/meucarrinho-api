package app.meucarrinho.domain.account;

import app.meucarrinho.domain.shared.CurrencyCode;

public record UserPreferences(
        SortOrder sortOrder,
        boolean collaborationAlerts,
        boolean haptics,
        boolean analyticsOptOut,
        CurrencyCode currency) {
    public static UserPreferences defaults() {
        return new UserPreferences(SortOrder.ADDED, true, true, true, CurrencyCode.BRL);
    }

    public UserPreferences apply(PreferencesChange change) {
        return new UserPreferences(
                change.sortOrder().orElse(sortOrder),
                change.collaborationAlerts().orElse(collaborationAlerts),
                change.haptics().orElse(haptics),
                change.analyticsOptOut().orElse(analyticsOptOut),
                currency);
    }
}
