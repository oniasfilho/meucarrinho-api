package app.meucarrinho.api.rest.me;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.RestTest;
import app.meucarrinho.domain.account.SortOrder;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.util.UUID;
import app.meucarrinho.testfixtures.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

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

    @Test
    void the_first_sign_in_creates_the_account_and_later_ones_refresh_it() {
        actor.signInWithoutAccount(new ExternalRef("auth0", "new-user|" + UUID.randomUUID()));
        MeApi api = client(MeApi.class);

        MeResponse created = api.upsert(new UpsertMeRequest("Tia Rosa", "rosa@example.com"));
        MeResponse again = api.upsert(new UpsertMeRequest("Rosa", null));

        assertThat(created.displayName()).isEqualTo("Tia Rosa");
        assertThat(again.id()).isEqualTo(created.id());
        assertThat(again.displayName()).isEqualTo("Rosa");
    }

    @Test
    void sign_in_needs_a_token_and_a_valid_name() {
        assertThat(http.put().uri("/v1/me").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Rosa\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        actor.signInWithoutAccount(new ExternalRef("auth0", "new-user|" + UUID.randomUUID()));
        assertThat(http.get().uri("/v1/me")).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.message").asString().contains("PUT /v1/me");
        assertThat(http.put().uri("/v1/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("displayName");
        assertThat(http.put().uri("/v1/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Rosa\",\"email\":\"not-an-email\"}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("email");
    }
}
