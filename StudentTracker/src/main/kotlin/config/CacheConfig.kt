package org.example.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager("studentsCache")
        caffeineCacheManager.setCaffeine(caffeineCacheBuilder())
        return caffeineCacheManager
    }

    private fun caffeineCacheBuilder(): Caffeine<Any, Any> {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
    }
}