package app.meucarrinho.api.rest.me;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.RestTest;
import app.meucarrinho.domain.account.SortOrder;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.testfixtures.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MeApiTest extends RestTest {
    @Test
    void the_caller_reads_their_profile_and_changes_one_preference_at_a_time() {
        AccountId me = signIn("Onias");
        MeApi api = client(MeApi.class);

        MeResponse profile = api.me();
        assertThat(profile.id()).isEqualTo(me.value());
        assertThat(profile.displayName()).isEqualTo("Onias");
        assertThat(profile.preferences().analyticsOptOut()).as("opted out by default (spec §9)").isTrue();

        MeResponse changed = api.updatePreferences(new PreferencesPatch(SortOrder.AZ, null, null, null));

        assertThat(changed.preferences().sortOrder()).isEqualTo(SortOrder.AZ);
        assertThat(changed.preferences().haptics()).isTrue();
        assertThat(api.me().preferences().sortOrder()).isEqualTo(SortOrder.AZ);
    }

    @Test
    void a_caller_without_an_account_is_told_so_and_an_anonymous_one_is_unauthenticated() {
        actor.signInAs(TestIds.accountId());
        assertThat(http.get().uri("/v1/me")).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_NOT_FOUND");

        actor.signOut();
        assertThat(http.get().uri("/v1/me")).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }
}
