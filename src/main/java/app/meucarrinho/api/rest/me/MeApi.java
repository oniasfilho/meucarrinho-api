package app.meucarrinho.api.rest.me;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

/**
 * The caller's account (spec §7). {@code PUT /v1/me} arrives with authentication in step 5; capabilities join
 * {@code GET /v1/me} in session 4, a compatible change (spec §13).
 */
@HttpExchange("/v1/me")
public interface MeApi {
    @GetExchange
    MeResponse me();

    /** Absent or null fields keep their value. */
    @PatchExchange("/preferences")
    MeResponse updatePreferences(@Valid @RequestBody PreferencesPatch patch);
}
