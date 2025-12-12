package org.exp.primeapp.configs.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "PRIME77 e-commerce", version = "v2", description = "Online e-commerce webapp")
)
public class OpenApiConfig {

    @Value("${swagger.server.url}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        final String SCHEME_NAME = "Authorization";
        return new OpenAPI()
                .addServersItem(new Server()
                        .url(serverUrl)
                        .description("Production Server"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                ));
    }

    @Bean
    public GroupedOpenApi userAuthApi() {
        return GroupedOpenApi.builder()
                .group("user-auth")
                .pathsToMatch(
                        "/api/v2/auth/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminAuthApi() {
        return GroupedOpenApi.builder()
                .group("admin-auth")
                .pathsToMatch(
                        "/api/v1/admin/auth/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .pathsToMatch(
                        "/api/v1/user/**",
                        "/api/v1/product/**",
                        "/api/v1/products/**",
                        "/api/v1/category/**",
                        "/api/v1/categories/**",
                        "/api/v1/order/**",
                        "/api/v1/orders/**",
                        "/api/v1/attachment/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch(
                        "/api/v1/admin/**",
                        "/api/v2/admin/**"
                )
                .pathsToExclude(
                        "/api/v1/admin/auth/**" // auth allaqachon admin-auth group da
                )
                .build();
    }
}
