package az.yusif.fx.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static az.yusif.fx.web.filter.ApiKeyFilter.API_KEY_HEADER;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI fxOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CBAR FX Service")
                        .version("v1")
                        .description("Official AZN exchange rates published by the Central Bank of Azerbaijan"))
                .components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(API_KEY_HEADER)))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
