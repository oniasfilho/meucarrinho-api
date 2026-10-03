package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sign-in end to end over real HTTP, as Postman does it (spec §6, §14): a token from mock-oauth2, verified by the
 * resource server, mapped to the seeded account.
 */
@SpringBootTest(classes = MeuCarrinhoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TokenSignInIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Container
    private static final GenericContainer<?> MOCK_OAUTH2 =
            new GenericContainer<>("ghcr.io/navikt/mock-oauth2-server:2.1.10").withExposedPorts(8080);

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.modulith.events.jdbc.schema-initialization.enabled", () -> false);
        properties.add("spring.aop.proxy-target-class", () -> false);
        properties.add("carrinho.adapters.idempotency", () -> "postgres");
        properties.add("carrinho.seed.enabled", () -> true);
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", TokenSignInIntegrationTest::issuer);
    }

    private static String issuer() {
        return "http://" + MOCK_OAUTH2.getHost() + ":" + MOCK_OAUTH2.getMappedPort(8080) + "/default";
    }

    private static String token(String user, String audience) throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(issuer() + "/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=client_credentials&client_id=" + user + "&client_secret=local&scope=" + audience))
                .build(), HttpResponse.BodyHandlers.ofString());
        Matcher token = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        assertThat(token.find()).as(response.body()).isTrue();
        return token.group(1);
    }

    private HttpResponse<String> me(String bearer) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/me"));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void a_mock_oauth2_token_for_marina_is_the_seeded_marina() throws Exception {
        HttpResponse<String> response = me(token("marina", "carrinho-api"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"displayName\":\"Marina\"");
    }

    @Test
    void no_token_a_wrong_audience_and_garbage_are_all_401() throws Exception {
        assertThat(me(null).statusCode()).isEqualTo(401);
        assertThat(me(token("marina", "some-other-api")).statusCode()).isEqualTo(401);
        assertThat(me("abc.def.ghi").body()).contains("\"code\":\"UNAUTHENTICATED\"");
    }
}
