package com.costumi.backend.identidad.dominio;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Empresa: puras, sin BD ni Spring (CLAUDE.md: "test primero"). */
class EmpresaTest {

	@Test
	void una_empresa_nueva_nace_pendiente() {
		Empresa empresa = Empresa.registrar("Disfraces Pirata");

		assertThat(empresa.estado()).isEqualTo(EstadoEmpresa.PENDIENTE);
		assertThat(empresa.nombre()).isEqualTo("Disfraces Pirata");
		assertThat(empresa.id()).isNotNull();
		assertThat(empresa.fechaRegistro()).isNotNull();
	}

	@Test
	void aprobar_una_pendiente_la_activa() {
		Empresa empresa = Empresa.registrar("X");

		empresa.aprobar();

		assertThat(empresa.estado()).isEqualTo(EstadoEmpresa.ACTIVA);
	}

	@Test
	void rechazar_una_pendiente_la_deja_rechazada() {
		Empresa empresa = Empresa.registrar("X");

		empresa.rechazar();

		assertThat(empresa.estado()).isEqualTo(EstadoEmpresa.RECHAZADA);
	}

	@Test
	void no_se_puede_aprobar_una_ya_rechazada() {
		Empresa empresa = Empresa.registrar("X");
		empresa.rechazar();

		assertThatThrownBy(empresa::aprobar)
				.isInstanceOf(TransicionDeEstadoInvalida.class);
	}

	@Test
	void suspender_una_activa_y_luego_reactivarla() {
		Empresa empresa = Empresa.registrar("X");
		empresa.aprobar();

		empresa.suspender();
		assertThat(empresa.estado()).isEqualTo(EstadoEmpresa.SUSPENDIDA);

		empresa.reactivar();
		assertThat(empresa.estado()).isEqualTo(EstadoEmpresa.ACTIVA);
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Empresa.registrar("   "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void una_solicitud_reciente_no_esta_vencida() {
		Empresa empresa = Empresa.registrar("X");

		assertThat(empresa.solicitudVencida(Duration.ofDays(2), Instant.now())).isFalse();
	}

	@Test
	void una_pendiente_pasado_el_plazo_esta_vencida() {
		Instant hace3Dias = Instant.now().minus(Duration.ofDays(3));
		Empresa empresa = Empresa.rehidratar(UUID.randomUUID(), "X", EstadoEmpresa.PENDIENTE, hace3Dias, null, null, null);

		assertThat(empresa.solicitudVencida(Duration.ofDays(2), Instant.now())).isTrue();
	}

	@Test
	void una_empresa_no_pendiente_nunca_esta_vencida() {
		Instant hace3Dias = Instant.now().minus(Duration.ofDays(3));
		Empresa empresa = Empresa.rehidratar(UUID.randomUUID(), "X", EstadoEmpresa.ACTIVA, hace3Dias, null, null, null);

		assertThat(empresa.solicitudVencida(Duration.ofDays(2), Instant.now())).isFalse();
	}
}
