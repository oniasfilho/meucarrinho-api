package br.com.oniasfilho.meucarrinho.common.storage;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(S3StorageProperties.class)
class S3StorageConfig {

    @Bean
    S3Client s3Client(S3StorageProperties props) {
        var builder = S3Client.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(props.pathStyleAccess())
                .build());
        if (props.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(S3StorageProperties props) {
        var builder = S3Presigner.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(props.pathStyleAccess())
                .build());
        if (props.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    private static AwsCredentialsProvider credentialsProvider(S3StorageProperties props) {
        if (props.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
