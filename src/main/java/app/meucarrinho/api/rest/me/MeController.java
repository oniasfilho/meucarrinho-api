package app.meucarrinho.api.rest.me;

import static app.meucarrinho.api.rest.ApiErrors.unwrap;

import app.meucarrinho.api.rest.ApiErrors;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.api.rest.Fields;
import app.meucarrinho.application.accounts.UpdatePreferences;
import app.meucarrinho.application.accounts.UpsertAccount;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import java.util.Optional;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController implements MeApi {
    private final CurrentActor caller;
    private final GetAccount getAccount;
    private final UpdatePreferences updatePreferences;
    private final UpsertAccount upsertAccount;

    MeController(CurrentActor caller, GetAccount getAccount, UpdatePreferences updatePreferences,
            UpsertAccount upsertAccount) {
        this.caller = caller;
        this.getAccount = getAccount;
        this.updatePreferences = updatePreferences;
        this.upsertAccount = upsertAccount;
    }

    @Override
    public MeResponse upsert(UpsertMeRequest request) {
        var identity = caller.requireIdentity();
        var name = Fields.parse("displayName", () -> new DisplayName(Fields.required("displayName", request.displayName())));
        Optional<EmailAddress> email = Optional.ofNullable(request.email())
                .map(value -> Fields.parse("email", () -> new EmailAddress(value)));
        return MeResponse.of(unwrap(upsertAccount.upsert(identity, name, email), ApiErrors::of));
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
