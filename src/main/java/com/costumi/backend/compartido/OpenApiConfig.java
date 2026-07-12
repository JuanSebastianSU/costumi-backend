package com.costumi.backend.compartido;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
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

	/**
	 * Documenta el cuerpo de error de la API en el contrato: el {@code ProblemDetail} (RFC 7807) que devuelven
	 * todos los {@code @ExceptionHandler}. Registra el schema y le suma a cada operación una respuesta
	 * {@code default} con {@code application/problem+json}, para que el cliente Kotlin tipe los errores.
	 */
	@Bean
	OpenApiCustomizer respuestasDeErrorProblemDetail() {
		return openApi -> {
			if (openApi.getComponents() == null || openApi.getPaths() == null) {
				return;
			}
			openApi.getComponents().addSchemas("ProblemDetail", esquemaProblemDetail());
			Content contenido = new Content().addMediaType("application/problem+json",
					new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")));
			ApiResponse respuestaDeError = new ApiResponse()
					.description("Error de la API en formato RFC 7807 (application/problem+json).")
					.content(contenido);
			openApi.getPaths().values().forEach(item -> item.readOperations().forEach(op -> {
				if (op.getResponses() != null && op.getResponses().get("default") == null) {
					op.getResponses().addApiResponse("default", respuestaDeError);
				}
			}));
		};
	}

	/** Schema RFC 7807 (los campos que Spring pone en {@link org.springframework.http.ProblemDetail}). */
	private static Schema<?> esquemaProblemDetail() {
		return new ObjectSchema()
				.description("Detalle de un problema según RFC 7807.")
				.addProperty("type", new StringSchema().format("uri").description("URI que identifica el tipo de error."))
				.addProperty("title", new StringSchema().description("Resumen legible del tipo de error."))
				.addProperty("status", new IntegerSchema().format("int32").description("Código de estado HTTP."))
				.addProperty("detail", new StringSchema().description("Explicación específica de esta ocurrencia."))
				.addProperty("instance", new StringSchema().format("uri").description("URI del recurso afectado."));
	}
}
