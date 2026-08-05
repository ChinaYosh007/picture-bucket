package com.yosh.server.config;

import com.yosh.common.model.core.MailCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    private final String mailFrom;

    public MailConfig(@Value("${spring.mail.username}") String mailFrom) {
        this.mailFrom = mailFrom;
    }

    @Bean
    public MailCore mailCore() {
        return MailCore.builder()
                .mailFrom(mailFrom)
                .build();
    }
}