package com.sigeclin.config;

import com.sigeclin.seguridad.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sigeclin.seguridad.service.LoginRateLimiterService loginRateLimiterService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (loginRateLimiterService != null) {
            com.sigeclin.seguridad.filter.LoginRateLimitingFilter filter = 
                new com.sigeclin.seguridad.filter.LoginRateLimitingFilter(loginRateLimiterService);
            http.addFilterBefore(filter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        }
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/webjars/**", "/api/cie10/**", "/api/auth/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)
            .formLogin((form) -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout((logout) -> logout.permitAll())
            .sessionManagement(session -> session
                .invalidSessionUrl("/login?invalid")
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired")
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
            )
            .headers(headers -> headers
                .xssProtection(xss -> xss
                    .headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; style-src 'self' https://fonts.googleapis.com https://cdn.jsdelivr.net 'unsafe-inline'; font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; img-src 'self' data:; connect-src 'self' https://cdn.jsdelivr.net")
                )
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        String idForEncode = "argon2";
        java.util.Map<String, PasswordEncoder> encoders = new java.util.HashMap<>();
        encoders.put(idForEncode, org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("bcrypt", new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        
        org.springframework.security.crypto.password.DelegatingPasswordEncoder passwordEncoder = 
            new org.springframework.security.crypto.password.DelegatingPasswordEncoder(idForEncode, encoders);
        
        // Use BCrypt for old hashes that don't have an {id} prefix
        passwordEncoder.setDefaultPasswordEncoderForMatches(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        
        return passwordEncoder;
    }
}
