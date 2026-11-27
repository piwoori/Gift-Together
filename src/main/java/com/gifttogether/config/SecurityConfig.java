
package com.gifttogether.config;

import com.gifttogether.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] API_DOCUMENTATION_PATHS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                    if (environment.acceptsProfiles(Profiles.of("dev & !prod"))) {
                        auth.requestMatchers(API_DOCUMENTATION_PATHS).permitAll();
                    } else {
                        auth.requestMatchers(API_DOCUMENTATION_PATHS).denyAll();
                    }
                    auth.requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/wishlist")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/fundings/my")
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/fundings/*/contributions"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/fundings/*")
                        .permitAll()
                        .anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED
                                )
                        )
                        .accessDeniedHandler((request, response, e) ->
                                response.sendError(
                                        HttpServletResponse.SC_FORBIDDEN
                                )
                        )
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
