package com.costumi.backend.identidad.adaptadores.entrada;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad de la API: stateless, JWT HS256 (RF-17.4 / §5.6) con autorización por rol.
 *
 * <p>Reglas (cierra la deuda de autorización):
 * <ul>
 *   <li>Público: login y auto-registro de Empresa (RF-15.2), health/info.</li>
 *   <li>SUPERADMIN: ciclo de vida de Empresa (RF-15.3) y cola de pendientes (RF-15.4).</li>
 *   <li>DUENO/ENCARGADO: alta de Sucursal (el dueño del tenant se valida además en el controller).</li>
 *   <li>Resto: requiere autenticación.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/registro",
								"/api/v1/auth/olvide", "/api/v1/auth/restablecer").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/empresas").permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/marketplace/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/empresas/pendientes").hasRole("SUPERADMIN")
						.requestMatchers(HttpMethod.POST,
								"/api/v1/empresas/*/aprobar", "/api/v1/empresas/*/rechazar",
								"/api/v1/empresas/*/suspender", "/api/v1/empresas/*/reactivar").hasRole("SUPERADMIN")
						.requestMatchers(HttpMethod.POST, "/api/v1/empresas/*/sucursales").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/empleados").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.GET, "/api/v1/empleados/*/permisos").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.PUT, "/api/v1/empleados/*/permisos").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.GET, "/api/v1/empleados/*/sucursales").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.PUT, "/api/v1/empleados/*/sucursales").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.GET, "/api/v1/empleados/*/actividad").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/categorias").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/tipos-etiqueta").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/tipos-etiqueta/*/valores").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/tipos-etiqueta/**").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/prendas").hasAnyRole("DUENO", "ENCARGADO", "BODEGA")
						.requestMatchers(HttpMethod.POST, "/api/v1/prendas/*/foto").hasAnyRole("DUENO", "ENCARGADO", "BODEGA")
						.requestMatchers(HttpMethod.POST, "/api/v1/prendas/*/grupos-stock").hasAnyRole("DUENO", "ENCARGADO", "BODEGA")
						.requestMatchers(HttpMethod.POST, "/api/v1/grupos-stock/*/mover").hasAnyRole("DUENO", "ENCARGADO", "BODEGA")
						.requestMatchers(HttpMethod.POST, "/api/v1/grupos-stock/*/entrada").hasAnyRole("DUENO", "ENCARGADO", "BODEGA")
						.requestMatchers(HttpMethod.POST, "/api/v1/grupos-stock/*/ajuste").hasAnyRole("DUENO", "ENCARGADO", "BODEGA")
						.requestMatchers(HttpMethod.POST, "/api/v1/disfraces").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/clientes/*/lista-negra").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/clientes").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.PUT, "/api/v1/clientes/*/device-token").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/carritos/items", "/api/v1/carritos/checkout",
								"/api/v1/carritos/checkout-renta")
						.hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION", "CLIENTE")
						.requestMatchers(HttpMethod.POST, "/api/v1/rentas", "/api/v1/rentas/*/entregar",
								"/api/v1/rentas/*/devolver", "/api/v1/rentas/*/cerrar", "/api/v1/rentas/*/cancelar",
								"/api/v1/rentas/*/extender")
						.hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/devoluciones").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/ventas").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/ventas/*/devolver").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/pagos/webhook").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/pagos/intento").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/pagos").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.POST, "/api/v1/caja/turnos", "/api/v1/caja/turnos/*/movimientos",
								"/api/v1/caja/turnos/*/cerrar").hasAnyRole("DUENO", "ENCARGADO", "MOSTRADOR", "ATENCION")
						.requestMatchers(HttpMethod.GET, "/api/v1/reportes/**").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.GET, "/api/v1/auditoria").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.PUT, "/api/v1/configuracion").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/configuracion/import").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/notificaciones/recordar-vencidas").hasAnyRole("DUENO", "ENCARGADO")
						.requestMatchers(HttpMethod.POST, "/api/v1/notificaciones").hasAnyRole("DUENO", "ENCARGADO", "ATENCION")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeAutoridades())));
		return http.build();
	}

	/** Mapea el claim {@code rol} del token a la autoridad {@code ROLE_<rol>} de Spring Security. */
	private JwtAuthenticationConverter convertidorDeAutoridades() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			String rol = jwt.getClaimAsString("rol");
			if (rol == null || rol.isBlank()) {
				return List.<GrantedAuthority>of();
			}
			return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + rol));
		});
		return converter;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecretKey jwtSecretKey(@Value("${costumi.security.jwt.secret}") String secret) {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey,
			@Value("${costumi.security.jwt.issuer:costumi}") String issuer) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
		return decoder;
	}
}
