package com.costumi.backend.identidad.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Sucursal: puras, sin BD ni Spring. */
class SucursalTest {

	@Test
	void crear_una_sucursal_valida() {
		UUID empresaId = UUID.randomUUID();

		Sucursal sucursal = Sucursal.crear(empresaId, "Sucursal Centro", "Av. Siempre Viva 123");

		assertThat(sucursal.id()).isNotNull();
		assertThat(sucursal.empresaId()).isEqualTo(empresaId);
		assertThat(sucursal.nombre()).isEqualTo("Sucursal Centro");
		assertThat(sucursal.direccion()).isEqualTo("Av. Siempre Viva 123");
	}

	@Test
	void la_direccion_es_opcional() {
		Sucursal sucursal = Sucursal.crear(UUID.randomUUID(), "Sin dirección", "   ");

		assertThat(sucursal.direccion()).isNull();
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Sucursal.crear(UUID.randomUUID(), "  ", null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
