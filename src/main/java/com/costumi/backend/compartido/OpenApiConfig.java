package com.costumi.backend.compartido;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI del backend (RF-17.3, §5.6). springdoc genera el documento en {@code /v3/api-docs}
 * y la UI en {@code /swagger-ui.html}. Este es el <b>tooling contract-first</b>: la fuente única de la
 * que, al cerrar el backend (tras la Tanda 3), se generará el cliente Kotlin. Documenta el esquema de
 * seguridad JWT (bearer) que usan todos los endpoints salvo los públicos.
 */
@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI costumiOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Costumi API")
						.version("v1")
						.description("Marketplace multi-tenant de renta y venta de disfraces (Plataforma → Empresa → Sucursal)."))
				.components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}
}
