package app.meucarrinho.api.rest.me;

import static app.meucarrinho.api.rest.ApiErrors.unwrap;

import app.meucarrinho.api.rest.ApiErrors;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.application.accounts.UpdatePreferences;
import app.meucarrinho.domain.shared.AccountId;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController implements MeApi {
    private final CurrentActor caller;
    private final GetAccount getAccount;
    private final UpdatePreferences updatePreferences;

    MeController(CurrentActor caller, GetAccount getAccount, UpdatePreferences updatePreferences) {
        this.caller = caller;
        this.getAccount = getAccount;
        this.updatePreferences = updatePreferences;
    }

    @Override
    public MeResponse me() {
        AccountId me = caller.require();
        return MeResponse.of(unwrap(getAccount.byId(me), ApiErrors::of));
    }

    @Override
    public MeResponse updatePreferences(PreferencesPatch patch) {
        AccountId me = caller.require();
        return MeResponse.of(unwrap(updatePreferences.update(me, patch.toChange()), ApiErrors::of));
    }
}
