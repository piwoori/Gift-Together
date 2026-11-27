package com.gifttogether.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gifttogether.GiftTogetherApplication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DeploymentProfilePostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gift_profiles_test");

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod", ""})
    void servesJwtApisAndExposesDocumentationOnlyInDevelopment(String profile) throws Exception {
        // Command-line settings pin both connection paths to a new test container.
        try (var context = (ServletWebServerApplicationContext) new SpringApplicationBuilder(GiftTogetherApplication.class)
                .run("--spring.config.location=file:./src/main/resources/",
                        "--spring.profiles.active=" + profile,
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--spring.flyway.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.flyway.user=" + POSTGRES.getUsername(),
                        "--spring.flyway.password=" + POSTGRES.getPassword(),
                        "--jwt.secret=" + Base64.getEncoder().encodeToString(new byte[32]),
                        "--server.address=127.0.0.1", "--server.port=0")) {
            String base = "http://127.0.0.1:" + context.getWebServer().getPort();
            assertThat(context.getBeansOfType(InMemoryUserDetailsManager.class)).isEmpty();
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(context.getEnvironment().getProperty("spring.jpa.show-sql")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
            assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");

            var docs = get(base, "/v3/api-docs", null);
            var swagger = get(base, "/swagger-ui/index.html", null);
            if (profile.equals("dev")) {
                assertThat(docs.statusCode()).isEqualTo(200);
                assertThat(json.readTree(docs.body()).has("openapi")).isTrue();
                assertThat(swagger.statusCode()).isEqualTo(200);
                assertThat(swagger.body()).contains("Swagger UI");
            } else {
                assertThat(docs.statusCode()).isIn(401, 403, 404);
                assertThat(swagger.statusCode()).isIn(401, 403, 404);
                assertThat(docs.body()).doesNotContain("\"openapi\"");
                assertThat(swagger.body()).doesNotContain("Swagger UI");
            }

            var jdbc = context.getBean(JdbcTemplate.class);
            Long product = jdbc.queryForObject("insert into products(name, price) values (?, ?) returning id",
                    Long.class, "profile-qa-" + UUID.randomUUID(), 3_000L);
            String owner = createAndLogin(base);
            String participant = createAndLogin(base);
            assertThat(get(base, "/api/products", null).statusCode()).isEqualTo(401);
            assertThat(get(base, "/api/products", owner).statusCode()).isEqualTo(200);
            assertThat(post(base, "/api/wishlist", Map.of("productId", product), owner, null).statusCode()).isEqualTo(201);

            var funding = post(base, "/api/fundings", Map.of("productId", product,
                    "expiredAt", LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusHours(1).withNano(0).toString()), owner, null);
            assertThat(funding.statusCode()).isEqualTo(201);
            String path = "/api/fundings/" + json.readTree(funding.body()).get("fundingId").asLong();
            assertThat(get(base, path, null).statusCode()).isEqualTo(200);
            assertThat(post(base, path + "/cancel", Map.of(), participant, null).statusCode()).isEqualTo(403);
            assertThat(post(base, path + "/cancel", Map.of(), participant, null).body()).doesNotContain("trace", "exception");

            var original = post(base, path + "/contributions", Map.of("amount", 1_000), participant, "profile-qa");
            assertThat(original.statusCode()).isEqualTo(201);
            var retry = post(base, path + "/contributions", Map.of("amount", 1_000), participant, "profile-qa");
            assertThat(retry.body()).isEqualTo(original.body());
            var recovered = read(base, path + "/contributions/idempotency", participant, "profile-qa");
            assertThat(recovered.statusCode()).isEqualTo(200);
            assertThat(json.readTree(recovered.body()).get("result")).isEqualTo(json.readTree(original.body()));
            long id = json.readTree(original.body()).get("contributionId").asLong();
            assertThat(jdbc.queryForObject("select count(*) from payments where contribution_id=? and status='SUCCESS' and amount=1000",
                    Integer.class, id)).isEqualTo(1);
        }
    }

    private String createAndLogin(String base) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String password = UUID.randomUUID().toString();
        assertThat(post(base, "/api/auth/signup", Map.of("nickname", "profile-qa", "email", email, "password", password),
                null, null).statusCode()).isEqualTo(201);
        var login = post(base, "/api/auth/login", Map.of("email", email, "password", password), null, null);
        assertThat(login.statusCode()).isEqualTo(200);
        JsonNode token = json.readTree(login.body()).get("accessToken");
        assertThat(token.asText().isBlank()).isFalse();
        return token.asText();
    }

    private HttpResponse<String> get(String base, String path, String token) throws Exception {
        return read(base, path, token, null);
    }

    private HttpResponse<String> read(String base, String path, String token, String key) throws Exception {
        return http.send(request(base, path, token, key).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String base, String path, Map<String, ?> body, String token, String key)
            throws Exception {
        return http.send(request(base, path, token, key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String base, String path, String token, String key) {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (key != null) builder.header("Idempotency-Key", key);
        return builder;
    }
}
