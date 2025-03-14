package gg.nya.imagehosting.services;

import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

@Service
@Scope("singleton")
public class S3Service {
    final Region region = Region.EU_NORTH_1;
    final String bucketName = "nya.gg";
    final S3Client s3Client = S3Client.builder().region(region).build();

    public InputStream getImage(String subdomain, String fileName) {
        String key = String.format("%s/%s", subdomain, fileName);
        try {
            InputStream stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            if (stream == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            }
            return stream;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
    }
}
