package com.costumi.backend.compartido;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.TipoEtiquetaRepository;
import com.costumi.backend.catalogo.dominio.ValorEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiquetaRepository;
import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aislamiento multi-tenant FORZADO (§5.4): el filtro Hibernate acota TODA consulta al {@code empresa_id}
 * del token, aunque el caso de uso consulte por un id ajeno. Se prueba a nivel de repositorio con un
 * método de consulta (los filtros aplican a queries, no a {@code find()} por PK), limpiando el caché de
 * primer nivel para forzar el golpe a BD.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AislamientoForzadoIntegrationTest {

	@Autowired
	EmpresaRepository empresas;

	@Autowired
	TipoEtiquetaRepository tipos;

	@Autowired
	ValorEtiquetaRepository valores;

	@PersistenceContext
	EntityManager em;

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	private static void comoTenant(UUID empresaId) {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
				.subject(UUID.randomUUID().toString())
				.claim("empresa_id", empresaId.toString()).claim("rol", "DUENO").build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
	}

	@Test
	void una_consulta_no_devuelve_datos_de_otro_tenant() {
		SecurityContextHolder.clearContext();
		Empresa empresaA = empresas.guardar(Empresa.registrar("Empresa A"));
		Empresa empresaB = empresas.guardar(Empresa.registrar("Empresa B"));
		TipoEtiqueta tipoDeA = tipos.guardar(TipoEtiqueta.crear(empresaA.id(), "Color", true, true));
		valores.guardar(ValorEtiqueta.crear(empresaA.id(), tipoDeA.id(), "Rojo"));
		em.flush();
		em.clear();

		// El tenant B, aun conociendo el id del tipo de A, no ve sus valores (filtro forzado).
		comoTenant(empresaB.id());
		assertThat(valores.listarPorTipo(tipoDeA.id())).isEmpty();

		// El tenant A sí ve los suyos.
		comoTenant(empresaA.id());
		assertThat(valores.listarPorTipo(tipoDeA.id())).hasSize(1);
	}
}
