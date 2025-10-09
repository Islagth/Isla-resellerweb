package com.example.enel_bitrix24_integration.config;
import java.security.SecureRandom;
import java.util.Base64;


public class ApiKeyGenerator {

    public static String generateApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bit di entropia
        random.nextBytes(bytes);
        // Codifica Base64 URL safe senza padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static void main(String[] args) {
        String apiKey = generateApiKey();
        System.out.println("API Key generata: " + apiKey);
    }
}
