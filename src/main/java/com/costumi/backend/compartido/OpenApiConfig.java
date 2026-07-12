package com.costumi.backend.compartido;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * Contrato OpenAPI del backend (RF-17.3, §5.6). springdoc genera el documento en {@code /v3/api-docs}
 * y la UI en {@code /swagger-ui.html}. Este es el <b>tooling contract-first</b>: la fuente única de la
 * que, al cerrar el backend (tras la Tanda 3), se generará el cliente Kotlin. Documenta el esquema de
 * seguridad JWT (bearer) que usan todos los endpoints salvo los públicos.
 */
@Configuration
class OpenApiConfig {

	/** POSTs públicos (permitAll en SecurityConfig): no exigen token. Deben reflejarse así en el contrato. */
	private static final Set<String> POSTS_PUBLICOS = Set.of(
			"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/auth/registro",
			"/api/v1/auth/olvide", "/api/v1/auth/restablecer", "/api/v1/empresas", "/api/v1/pagos/webhook");

	/** Prefijo de la vitrina pública del marketplace (GET, sin token). */
	private static final String PREFIJO_MARKETPLACE = "/api/v1/marketplace";

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

	/**
	 * Quita el requisito de seguridad global de los endpoints públicos, para que el contrato (y el cliente
	 * generado) no crea que login/refresh/registro/webhook/marketplace necesitan token. {@code security: []}
	 * a nivel operación sobreescribe el requisito global. Centralizado aquí para no anotar cada controller.
	 */
	@Bean
	OpenApiCustomizer endpointsPublicosSinSeguridad() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().forEach((ruta, item) -> {
				if (POSTS_PUBLICOS.contains(ruta) && item.getPost() != null) {
					item.getPost().setSecurity(List.of());
				}
				if (ruta.startsWith(PREFIJO_MARKETPLACE)) {
					PathItem.HttpMethod get = PathItem.HttpMethod.GET;
					item.readOperationsMap().forEach((metodo, op) -> {
						if (metodo == get) {
							op.setSecurity(List.of());
						}
					});
				}
			});
		};
	}
}
