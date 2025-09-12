package com.example.enel_bitrix24_integration.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enel")
public class EnelProperties {

    /**
     * Token di autenticazione fornito da Enel.
     * Caricato da application.yml o variabile d'ambiente.
     */
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
