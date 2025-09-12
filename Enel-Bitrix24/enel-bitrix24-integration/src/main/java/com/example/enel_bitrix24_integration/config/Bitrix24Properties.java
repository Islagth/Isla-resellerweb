package com.example.enel_bitrix24_integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bitrix24")
public class Bitrix24Properties {

    /**
     * URL del webhook REST di Bitrix24.
     */
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
