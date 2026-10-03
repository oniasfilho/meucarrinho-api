package app.meucarrinho.api.rest.me;

import app.meucarrinho.domain.account.PreferencesChange;
import app.meucarrinho.domain.account.SortOrder;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** {@code analyticsOptOut} is the product-analytics consent (spec §9); it starts {@code true}. */
public record PreferencesPatch(
        @Nullable SortOrder sortOrder,
        @Nullable Boolean collaborationAlerts,
        @Nullable Boolean haptics,
        @Nullable Boolean analyticsOptOut) {
    PreferencesChange toChange() {
        return new PreferencesChange(Optional.ofNullable(sortOrder), Optional.ofNullable(collaborationAlerts),
                Optional.ofNullable(haptics), Optional.ofNullable(analyticsOptOut));
    }
}
