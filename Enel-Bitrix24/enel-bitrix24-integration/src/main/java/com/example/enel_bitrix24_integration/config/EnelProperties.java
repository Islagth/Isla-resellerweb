package com.example.enel_bitrix24_integration.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "enel.api")
public class EnelProperties {

    private String baseUrl;
    private String authUrl;
    private String clientId;
    private String clientJwt;

}
