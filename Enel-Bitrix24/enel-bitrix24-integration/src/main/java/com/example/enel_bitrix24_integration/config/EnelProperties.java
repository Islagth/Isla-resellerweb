package com.example.enel_bitrix24_integration.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "enel.api")
public class EnelProperties {

    private String baseUrl;
    private String authUrl;
    private String clientId;
    private String clientJwt;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientJwt() {
        return clientJwt;
    }

    public void setClientJwt(String clientJwt) {
        this.clientJwt = clientJwt;
    }
}
