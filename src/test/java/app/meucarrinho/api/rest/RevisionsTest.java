package app.meucarrinho.api.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

class RevisionsTest {
    @Test
    void if_match_carries_the_version_quoted_or_bare() {
        assertThat(Revisions.expected("\"12\"")).isEqualTo(12);
        assertThat(Revisions.expected(" 12 ")).isEqualTo(12);
        assertThat(Revisions.etag(12)).isEqualTo("\"12\"");
    }

    @Test
    void a_missing_if_match_is_precondition_required() {
        assertThatThrownBy(() -> Revisions.expected(null)).isInstanceOfSatisfying(ApiProblem.class, problem -> {
            assertThat(problem.status()).isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
            assertThat(problem.code()).isEqualTo("PRECONDITION_REQUIRED");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "W/\"3\"", "\"1\", \"2\"", "-1", "abc", "\"\"", "1234567890123456789"})
    void anything_else_is_malformed(String ifMatch) {
        assertThatThrownBy(() -> Revisions.expected(ifMatch)).isInstanceOfSatisfying(ApiProblem.class,
                problem -> assertThat(problem.code()).isEqualTo("MALFORMED_REQUEST"));
    }
}
