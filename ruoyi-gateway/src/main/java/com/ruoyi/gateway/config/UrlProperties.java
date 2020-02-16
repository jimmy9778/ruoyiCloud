package com.ruoyi.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@ConfigurationProperties(prefix = "config")
@PropertySource("classpath:config/urlConfig.properties")
@Data
public class UrlProperties {
    private String url;
    private String cacheUrl;
}