package br.com.oniasfilho.meucarrinho.session.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.common.storage.S3StorageProperties;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionService;
import br.com.oniasfilho.meucarrinho.session.dto.CreateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.dto.CreateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.ItemLabelPhotoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ItemLabelPhotoIntegrationTest {

    // 1x1 transparent PNG
    private static final byte[] PNG = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    static final LocalStackContainer LOCALSTACK =
        new LocalStackContainer("localstack/localstack:4").withServices("s3");

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.s3.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("app.storage.s3.region", LOCALSTACK::getRegion);
        registry.add("app.storage.s3.access-key", LOCALSTACK::getAccessKey);
        registry.add("app.storage.s3.secret-key", LOCALSTACK::getSecretKey);
        registry.add("app.storage.s3.bucket", () -> "test-media");
        registry.add("app.storage.s3.path-style-access", () -> true);
        registry.add("app.storage.s3.auto-create-bucket", () -> true);
    }

    @Autowired
    private ShoppingSessionService sessionService;

    @Autowired
    private ShoppingSessionItemService itemService;

    @Autowired
    private ItemLabelPhotoService labelPhotoService;

    @Autowired
    private ShoppingSessionItemRepository itemRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3StorageProperties s3Props;

    @Test
    void storesObjectPersistsKeyServesUsablePresignedUrlAndDeletes() throws Exception {
        UUID itemId = newItemId();

        ItemLabelPhotoResponse uploaded = labelPhotoService.upload(
            itemRepository.findById(itemId).orElseThrow().getSession().getId(),
            itemId,
            new MockMultipartFile("file", "label.png", "image/png", PNG));

        assertThat(uploaded.labelPhotoKey())
            .startsWith("sessions/")
            .contains("/items/" + itemId + "/")
            .endsWith(".png");
        assertThat(uploaded.labelPhotoUrl()).contains(uploaded.labelPhotoKey());
        assertThat(uploaded.urlExpiresAt()).isNotNull();

        // object really landed in the bucket
        byte[] stored = s3Client.getObjectAsBytes(
            b -> b.bucket(s3Props.bucket()).key(uploaded.labelPhotoKey())).asByteArray();
        assertThat(stored).isEqualTo(PNG);

        // key persisted on the item row
        assertThat(itemRepository.findById(itemId).orElseThrow().getLabelPhotoKey())
            .isEqualTo(uploaded.labelPhotoKey());

        // presigned URL is directly usable against (fake) S3
        HttpResponse<byte[]> httpResponse = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(uploaded.labelPhotoUrl())).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).isEqualTo(PNG);

        // GET endpoint returns a fresh presigned URL
        UUID sessionId = itemRepository.findById(itemId).orElseThrow().getSession().getId();
        assertThat(labelPhotoService.get(sessionId, itemId).labelPhotoUrl()).isNotBlank();

        // DELETE clears the row and the object
        labelPhotoService.delete(sessionId, itemId);
        assertThat(itemRepository.findById(itemId).orElseThrow().getLabelPhotoKey()).isNull();
        assertThatThrownBy(() -> labelPhotoService.get(sessionId, itemId))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> s3Client.getObjectAsBytes(
            b -> b.bucket(s3Props.bucket()).key(uploaded.labelPhotoKey())))
            .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void rejectsNonImageUpload() {
        UUID itemId = newItemId();
        UUID sessionId = itemRepository.findById(itemId).orElseThrow().getSession().getId();

        assertThatThrownBy(() -> labelPhotoService.upload(sessionId, itemId,
            new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private UUID newItemId() {
        ShoppingSessionDetailResponse session = sessionService.create(
            new CreateShoppingSessionRequest("S3 IT " + UUID.randomUUID(), null, new BigDecimal("100.00")));
        ShoppingSessionDetailResponse withItem = itemService.create(
            session.id(),
            new CreateShoppingSessionItemRequest("Arroz", new BigDecimal("9.90"), 1, null));
        return withItem.items().get(0).id();
    }
}
