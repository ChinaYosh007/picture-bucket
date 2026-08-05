package com.yosh.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/** Redis Session 的浏览器 Cookie 配置。 */
@Configuration
public class RedisSessionConfig {

    private static final int SESSION_COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    @Bean
    public CookieSerializer pictureBucketCookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("PICTURE_BUCKET_SESSION");
        serializer.setCookieMaxAge(SESSION_COOKIE_MAX_AGE_SECONDS);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }
}
