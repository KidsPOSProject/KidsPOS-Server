package info.nukoneko.kidspos.server.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/Swagger configuration for API documentation
 *
 * Configures the OpenAPI specification for the KidsPOS Server API,
 * providing comprehensive documentation for all REST endpoints.
 *
 * Part of Task 8.2: OpenAPI specification integration
 */
@Configuration
class OpenApiConfig {
    /**
     * Configure OpenAPI specification details
     *
     * @return OpenAPI configuration with metadata
     */
    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("KidsPOS Server API")
                    .version("1.0.0")
                    .description(
                        """
                        KidsPOS (キッズPOS) is a simplified Point of Sale system designed for
                        educational and entertainment purposes. This API provides endpoints for
                        managing sales, inventory, staff, and store operations.
                        """.trimIndent(),
                    ).contact(
                        Contact()
                            .name("KidsPOS Development Team")
                            .email("support@kidspos.example.com"),
                    ).license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT"),
                    ),
            ).addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Local development server"),
            )
}
