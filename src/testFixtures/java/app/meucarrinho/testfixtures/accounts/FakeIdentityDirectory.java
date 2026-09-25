package app.meucarrinho.testfixtures.accounts;

import app.meucarrinho.application.accounts.port.IdentityDirectory;
import app.meucarrinho.application.accounts.port.IdentityError;
import app.meucarrinho.application.accounts.port.IdentityProfile;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class FakeIdentityDirectory implements IdentityDirectory {
    private final Map<ExternalRef, IdentityProfile> profiles = new HashMap<>();
    private final Set<ExternalRef> disabled = new HashSet<>();

    public FakeIdentityDirectory add(IdentityProfile profile) {
        profiles.put(profile.subject(), profile);
        return this;
    }

    @Override
    public Result<IdentityProfile, IdentityError> profile(ExternalRef subject) {
        IdentityProfile profile = profiles.get(subject);
        return profile == null ? Result.err(new IdentityError.NotFound()) : Result.ok(profile);
    }

    @Override
    public Result<@Nullable Void, IdentityError> disable(ExternalRef subject) {
        if (!profiles.containsKey(subject)) {
            return Result.err(new IdentityError.NotFound());
        }
        disabled.add(subject);
        return Result.ok();
    }

    @Override
    public Result<@Nullable Void, IdentityError> delete(ExternalRef subject) {
        profiles.remove(subject);
        disabled.remove(subject);
        return Result.ok();
    }

    public boolean exists(ExternalRef subject) {
        return profiles.containsKey(subject);
    }

    public boolean isDisabled(ExternalRef subject) {
        return disabled.contains(subject);
    }
}
