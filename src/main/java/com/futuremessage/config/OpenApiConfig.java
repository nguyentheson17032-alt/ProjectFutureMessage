package com.futuremessage.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI futureMessageOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Future Message API")
                        .version("v1")
                        .description("""
                                REST API for messages that stay locked until `unlockAt`.

                                **Try it in Swagger UI**
                                1. Call `POST /api/v1/auth/register` (or login).
                                2. Copy `accessToken` from the response.
                                3. Click **Authorize** and paste the token only (do **not** type `Bearer`).
                                4. Call User and Message endpoints. Swagger sends `Authorization: Bearer <token>`.

                                Access tokens last 15 minutes locally. When they expire, call `/api/v1/auth/refresh`.

                                **Admin** (`/api/v1/admin/**`) requires a JWT with role ADMIN. Regular users get 403.
                                """))
                .servers(List.of(new Server().url("/").description("This server")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token from /api/v1/auth/register or /login")));
    }
}
