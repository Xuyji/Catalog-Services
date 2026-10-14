package com.musicdata.catalog;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Caffeine-backed caching; sizes and TTLs live in application.yml (spring.cache.caffeine.spec). */
@Configuration
@EnableCaching
public class CacheConfig {}
