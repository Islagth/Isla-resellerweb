package com.example.enel_bitrix24_integration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Value("${webhook.api-key}")
    private String expectedApiKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .requiresChannel(channel -> channel.anyRequest().requiresSecure()) // Forza HTTPS
                .csrf(csrf -> csrf.ignoringRequestMatchers("/apienel-leads/webhook-notify")) // Disabilita CSRF su webhook
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/apienel-leads/webhook-notify").permitAll() // Permessi per webhook, autenticato via filtro custom
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiKeyAuthFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());
        // Puoi abilitare l'OAuth2 resource server per altri endpoint protetti

        return http.build();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(expectedApiKey);
    }

    public static class ApiKeyAuthFilter extends HttpFilter {
        private final String expectedApiKey;

        public ApiKeyAuthFilter(String expectedApiKey) {
            this.expectedApiKey = expectedApiKey;
        }

        @Override
        protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            String path = request.getRequestURI();

            // Applica controllo api key solo al path webhook
            if ("/apienel-leads/webhook-notify".equals(path)) {
                String apiKey = request.getHeader("api-auth-token");
                if (apiKey == null || !apiKey.equals(expectedApiKey)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("API Key non valida");
                    return;
                }
            }

            chain.doFilter(request, response);
        }
    }
}
