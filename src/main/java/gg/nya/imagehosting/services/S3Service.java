package gg.nya.imagehosting.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URI;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for S3-compatible object storage interactions.
 */
@Service
public class S3Service {
    @Value("${app.object-store.access-key}")
    private String accessKey;
    
    @Value("${app.object-store.secret-key}")
    private String secretKey;
    
    @Value("${app.object-store.endpoint}")
    private String endpoint;
    
    @Value("${app.object-store.bucket}")
    private String bucketName;

    @Value("${app.object-store.region}")
    private String region;

    @Value("${spring.profiles.active:dev}")
    private String env;

    private S3Client s3Client;

    final private static Logger log = LoggerFactory.getLogger(S3Service.class);

    /**
     * Initializes the S3 client with the provided configuration.
     */
    @PostConstruct
    private void initS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3ClientBuilder clientBuilder = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region));

        if(env.equals("dev")) {
            clientBuilder = clientBuilder.forcePathStyle(true);
        }

        s3Client = clientBuilder.build();
    }

    /**
     * Get the size of a file in the S3 bucket. Throws a 404 error if the file could not be found.
     * @param subdomain The subdomain of the file (username).
     * @param fileName The filename of the file.
     * @return The size of the file in bytes.
     */
    public long getFileSize(String subdomain, String fileName) {
        String key = getKeyName(subdomain, fileName);
        log.debug("getFileSize, retrieving file size for key {} from bucket {}", key, bucketName);

        try {
            return s3Client.headObject(builder -> builder
                    .bucket(bucketName)
                    .key(key)
                    .build()).contentLength();
        } catch (AwsServiceException | SdkClientException e) {
            log.error("getFileSize, could not retrieve file size for key {} from bucket {}", key, bucketName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    /**
     * Retrieves full file from S3 bucket. Throws a 404 error if the file could not be retrieved successfully.
     * Caches into memory - not to be used for large files.
     *
     * @param subdomain The subdomain of the file (username).
     * @param fileName  The filename of the file.
     * @return The file data as a byte array.
     */
    @Cacheable(value = "fileCache", key = "#subdomain + '/' + #fileName")
    public byte[] getCacheableFile(String subdomain, String fileName) {
        String key = getKeyName(subdomain, fileName);
        log.debug("getCacheableFile, cache miss - retrieving file with key {} from bucket {}", key, bucketName);

        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return objectBytes.asByteArray();
        } catch (AwsServiceException | SdkClientException e) {
            log.error("getCacheableFile, could not retrieve file with key {} from bucket {}", key, bucketName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    /**
     * Get file stream from S3 bucket with byte range. Throws a 404 error if the file could not be retrieved successfully.
     *
     * @param subdomain The subdomain of the file (username).
     * @param fileName The filename of the file.
     * @param start The start byte of the range.
     * @param end The end byte of the range.
     * @return The file as an InputStream.
     */
    public InputStream getFileStreamRange(String subdomain, String fileName, long start, long end) {
        String key = getKeyName(subdomain, fileName);
        log.debug("getFileStreamRange, retrieving file with key {} from bucket {} with range bytes={}-{}",
                key, bucketName, start, end);

        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .range("bytes=" + start + "-" + end)
                    .build());
        } catch (AwsServiceException | SdkClientException e) {
            log.error("getFileStreamRange, could not retrieve file with key {} from bucket {}", key, bucketName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    /**
     * Upload file to S3 bucket. Throws a 500 error if the file could not be uploaded successfully.
     *
     * @param subdomain  The subdomain of the image (username).
     * @param fileName   The filename of the image.
     * @param fileStream The image as an InputStream.
     * @param contentType The content type of the image.
     */
    public void uploadFile(String subdomain, String fileName, InputStream fileStream, String contentType) {
        String key = getKeyName(subdomain, fileName);
        log.debug("uploadFile, attempting to upload file with key {} to bucket {}", key, bucketName);

        try {
            s3Client.putObject(builder -> builder
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build(), RequestBody.fromInputStream(fileStream, fileStream.available()));
        } catch (AwsServiceException | SdkClientException | IOException e) {
            log.error("uploadFile, could not upload file with key {} to bucket {}", key, bucketName);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload file");
        }
    }

    /**
     * Get the full name of the key in the S3 bucket.
     *
     * @param subdomain The subdomain of the image (username).
     * @param fileName  The filename of the image.
     * @return The full name of the key in the S3 bucket.
     */
    private String getKeyName(String subdomain, String fileName) {
        return String.format("%s/%s", subdomain, fileName);
    }
}
