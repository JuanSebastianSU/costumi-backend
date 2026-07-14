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

		Sucursal sucursal = Sucursal.crear(empresaId, "Sucursal Centro", "Av. Siempre Viva 123",
				"https://maps.google.com/?q=centro");

		assertThat(sucursal.id()).isNotNull();
		assertThat(sucursal.empresaId()).isEqualTo(empresaId);
		assertThat(sucursal.nombre()).isEqualTo("Sucursal Centro");
		assertThat(sucursal.direccion()).isEqualTo("Av. Siempre Viva 123");
		assertThat(sucursal.ubicacionMaps()).isEqualTo("https://maps.google.com/?q=centro");
	}

	@Test
	void la_direccion_y_el_maps_son_opcionales() {
		Sucursal sucursal = Sucursal.crear(UUID.randomUUID(), "Sin dirección", "   ", "  ");

		assertThat(sucursal.direccion()).isNull();
		assertThat(sucursal.ubicacionMaps()).isNull();
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Sucursal.crear(UUID.randomUUID(), "  ", null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void una_sucursal_nace_activa() {
		assertThat(Sucursal.crear(UUID.randomUUID(), "Centro", null, null).archivada()).isFalse();
	}

	@Test
	void editar_actualiza_nombre_direccion_y_maps() {
		Sucursal sucursal = Sucursal.crear(UUID.randomUUID(), "Centro", "Calle 1", null);
		UUID id = sucursal.id();

		sucursal.editar("Centro Renovado", "Calle 2", "https://maps.google.com/?q=calle2");

		assertThat(sucursal.id()).isEqualTo(id);
		assertThat(sucursal.nombre()).isEqualTo("Centro Renovado");
		assertThat(sucursal.direccion()).isEqualTo("Calle 2");
		assertThat(sucursal.ubicacionMaps()).isEqualTo("https://maps.google.com/?q=calle2");
	}

	@Test
	void editar_exige_nombre() {
		Sucursal sucursal = Sucursal.crear(UUID.randomUUID(), "Centro", null, null);
		assertThatThrownBy(() -> sucursal.editar("  ", "Calle 2", null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void archivar_y_activar_una_sucursal() {
		Sucursal sucursal = Sucursal.crear(UUID.randomUUID(), "Centro", null, null);

		sucursal.archivar();
		assertThat(sucursal.archivada()).isTrue();

		sucursal.activar();
		assertThat(sucursal.archivada()).isFalse();
	}
}
