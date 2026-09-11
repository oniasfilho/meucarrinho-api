package br.com.oniasfilho.meucarrinho.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * On startup, makes sure the configured bucket exists. Keeps the local dev loop smooth
 * even if {@code docker compose}'s init hook did not run (e.g. container reused).
 */
@Component
class S3BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(S3BucketInitializer.class);

    private final S3Client s3Client;
    private final S3StorageProperties props;

    S3BucketInitializer(S3Client s3Client, S3StorageProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.autoCreateBucket()) {
            return;
        }
        String bucket = props.bucket();
        try {
            s3Client.headBucket(b -> b.bucket(bucket));
            log.info("Object storage bucket '{}' is ready.", bucket);
        } catch (NoSuchBucketException e) {
            createBucket(bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                createBucket(bucket);
            } else {
                throw notReachable(e);
            }
        } catch (SdkClientException e) {
            throw notReachable(e);
        }
    }

    private void createBucket(String bucket) {
        s3Client.createBucket(b -> b.bucket(bucket));
        log.info("Object storage bucket '{}' created.", bucket);
    }

    private IllegalStateException notReachable(Exception cause) {
        return new IllegalStateException(
            "Object storage is not reachable at " + props.endpoint()
                + "; is `docker compose up` running?",
            cause);
    }
}
