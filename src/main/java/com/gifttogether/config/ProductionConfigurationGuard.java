package com.gifttogether.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class ProductionConfigurationGuard implements EnvironmentPostProcessor, Ordered {

    private static final Pattern URL_CREDENTIALS = Pattern.compile(
            "(?i)//[^/?#]*@|[?&;](?:user(?:name)?|password)=");

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        boolean development = environment.acceptsProfiles(Profiles.of("dev"));
        if (production && development) {
            throw new IllegalStateException("The dev and prod profiles cannot be active together.");
        }

        String datasourceUrl = read(environment, "spring.datasource.url");
        String flywayUrl = read(environment, "spring.flyway.url");
        rejectInlineCredentials("spring.datasource.url", datasourceUrl);
        rejectInlineCredentials("spring.flyway.url", flywayUrl);
        if (!production) {
            if (!development) {
                if (isPostgres(datasourceUrl)) {
                    require(environment, "spring.datasource.password");
                }
                if (isPostgres(flywayUrl)) {
                    require(environment, environment.containsProperty("spring.flyway.password")
                            ? "spring.flyway.password" : "spring.datasource.password");
                }
            }
            return;
        }
        require(environment, "spring.datasource.url");
        require(environment, "spring.datasource.username");
        require(environment, "spring.datasource.password");
        if (environment.containsProperty("spring.flyway.password")) {
            require(environment, "spring.flyway.password");
        }

        String secret = require(environment, "jwt.secret");
        try {
            if (Base64.getDecoder().decode(secret).length >= 32) {
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Decoder exceptions can contain input details. Report only the configuration key.
        }
        throw new IllegalStateException("Production configuration requires jwt.secret to be a Base64 key of at least 32 bytes.");
    }

    private boolean isPostgres(String url) {
        return url != null && url.startsWith("jdbc:postgresql:");
    }

    private void rejectInlineCredentials(String key, String url) {
        if (url == null) {
            return;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalStateException("Application configuration has an invalid " + key + ".");
        }
        if (URL_CREDENTIALS.matcher(decoded).find()) {
            throw new IllegalStateException("Application configuration must supply credentials outside " + key + ".");
        }
    }

    private String require(ConfigurableEnvironment environment, String key) {
        String value = read(environment, key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Application configuration requires " + key + ".");
        }
        return value;
    }

    private String read(ConfigurableEnvironment environment, String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalStateException("Application configuration requires " + key + ".");
        }
    }
}
