package com.example.enel_bitrix24_integration.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class TokenResponse {
    private String access_token;
    private String refresh_token;
    private String client_endpoint;
    private String server_endpoint;
    private String domain;
    private Integer expires_in;
    private String member_id;
    private String scope;
    private String status;

}
