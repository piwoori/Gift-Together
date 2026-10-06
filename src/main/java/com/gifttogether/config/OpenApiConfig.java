package com.gifttogether.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "X-USER-ID";

    @Bean
    public OpenAPI giftTogetherOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gift Together API")
                        .description("""
                                여러 사용자가 하나의 위시리스트 상품에
                                금액을 나누어 참여하는 공동 선물 펀딩 서비스 API
                                """)
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("X-USER-ID")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("MVP용 Mock 사용자 ID")
                        ));
    }
}