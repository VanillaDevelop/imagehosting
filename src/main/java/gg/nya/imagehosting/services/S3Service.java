package gg.nya.imagehosting.services;

import gg.nya.imagehosting.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
@Scope("singleton")
public class S3Service {
    final Region region = Region.EU_NORTH_1;
    final String bucketName = "nya.gg";
    final S3Client s3Client = S3Client.builder().region(region).build();

    final private static Logger log = LoggerFactory.getLogger(S3Service.class);

    /**
     * Get image from S3 bucket. Throws a 404 error if the image could not be retrieved successfully.
     *
     * @param subdomain The subdomain of the image (username).
     * @param fileName  The filename of the image.
     * @return The image as an InputStream.
     */
    public InputStream getImage(String subdomain, String fileName) {
        String key = String.format("%s/%s", subdomain, fileName);
        try {
            log.debug("getImage, attempting to retrieve image with key {} from bucket {}", key, bucketName);
            InputStream stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            if (stream == null) {
                log.error("getImage, image with key {} not found in bucket {}", key, bucketName);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            }
            return stream;
        } catch (AwsServiceException | SdkClientException e) {
            log.error("getImage, could not retrieve image with key {} from bucket {}", key, bucketName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
    }

    /**
     * Upload image to S3 bucket. Throws a 500 error if the image could not be uploaded successfully.
     *
     * @param subdomain  The subdomain of the image (username).
     * @param fileName   The filename of the image.
     * @param fileStream The image as an InputStream.
     */
    public void uploadImage(String subdomain, String fileName, InputStream fileStream) {
        String key = String.format("%s/%s", subdomain, fileName);
        try {
            log.debug("uploadImage, attempting to upload image with key {} to bucket {}", key, bucketName);
            s3Client.putObject(builder -> builder
                    .bucket(bucketName)
                    .key(key)
                    .contentType(Utils.getMediaTypeFromFilename(fileName).toString())
                    .build(), RequestBody.fromInputStream(fileStream, fileStream.available()));
        } catch (AwsServiceException | SdkClientException | IOException e) {
            log.error("uploadImage, could not upload image with key {} to bucket {}", key, bucketName);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload image");
        }
    }
}
