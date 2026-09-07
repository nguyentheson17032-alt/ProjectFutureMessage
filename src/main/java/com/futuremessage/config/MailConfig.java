package com.futuremessage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bật {@link MailProperties}. {@code JavaMailSender} do Spring Boot tự tạo từ {@code spring.mail.*}.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
}
