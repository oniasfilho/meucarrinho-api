package br.com.oniasfilho.meucarrinho.common.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
    String endpoint,
    @DefaultValue("us-east-1") String region,
    String bucket,
    String accessKey,
    String secretKey,
    @DefaultValue("false") boolean pathStyleAccess,
    @DefaultValue("15m") Duration presignTtl,
    @DefaultValue("false") boolean autoCreateBucket
) {

    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank()
            && secretKey != null && !secretKey.isBlank();
    }
}
