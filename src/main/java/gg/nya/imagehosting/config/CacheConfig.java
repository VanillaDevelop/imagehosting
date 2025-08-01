package gg.nya.imagehosting.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("fileCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200) // Cache up to 200 files
                .expireAfterWrite(30, TimeUnit.MINUTES) // Cache entries expire after 30 minutes
                .recordStats());
        return cacheManager;
    }
}
