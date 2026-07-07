package com.costumi.backend.inventario.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del GrupoDeStock: puras, sin BD ni Spring. */
class GrupoDeStockTest {

	private static GrupoDeStock nuevo(int cantidadInicial) {
		return GrupoDeStock.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				CombinacionDeVariante.de(java.util.Map.of(UUID.randomUUID(), UUID.randomUUID())), cantidadInicial);
	}

	@Test
	void crear_deja_todo_disponible() {
		GrupoDeStock grupo = nuevo(8);

		assertThat(grupo.disponibles()).isEqualTo(8);
		assertThat(grupo.total()).isEqualTo(8);
		assertThat(grupo.danadas()).isZero();
	}

	@Test
	void mover_unidades_a_danadas() {
		GrupoDeStock grupo = nuevo(8);

		grupo.mover(EstadoUnidad.DISPONIBLE, EstadoUnidad.DANADA, 3);

		assertThat(grupo.disponibles()).isEqualTo(5);
		assertThat(grupo.danadas()).isEqualTo(3);
		assertThat(grupo.total()).isEqualTo(8);
	}

	@Test
	void no_se_pueden_mover_mas_unidades_de_las_que_hay() {
		GrupoDeStock grupo = nuevo(2);

		assertThatThrownBy(() -> grupo.mover(EstadoUnidad.DISPONIBLE, EstadoUnidad.PERDIDA, 5))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void mover_al_mismo_estado_es_invalido() {
		GrupoDeStock grupo = nuevo(2);

		assertThatThrownBy(() -> grupo.mover(EstadoUnidad.DISPONIBLE, EstadoUnidad.DISPONIBLE, 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void la_cantidad_inicial_no_puede_ser_negativa() {
		assertThatThrownBy(() -> nuevo(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void reabastecer_aumenta_disponibles() {
		GrupoDeStock grupo = nuevo(3);

		grupo.reabastecer(5);

		assertThat(grupo.disponibles()).isEqualTo(8);
		assertThat(grupo.total()).isEqualTo(8);
	}

	@Test
	void dar_de_baja_reduce_disponibles_y_total() {
		GrupoDeStock grupo = nuevo(8);

		grupo.darDeBaja(3);

		assertThat(grupo.disponibles()).isEqualTo(5);
		assertThat(grupo.total()).isEqualTo(5);
	}

	@Test
	void no_se_puede_dar_de_baja_mas_de_lo_disponible() {
		GrupoDeStock grupo = nuevo(2);

		assertThatThrownBy(() -> grupo.darDeBaja(5)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void mismaVariante_compara_por_combinacion() {
		UUID empresa = UUID.randomUUID();
		UUID sucursal = UUID.randomUUID();
		UUID prenda = UUID.randomUUID();
		UUID color = UUID.randomUUID();
		UUID rojo = UUID.randomUUID();
		UUID azul = UUID.randomUUID();

		GrupoDeStock rojoA = GrupoDeStock.crear(empresa, sucursal, prenda, CombinacionDeVariante.de(java.util.Map.of(color, rojo)), 3);
		GrupoDeStock rojoB = GrupoDeStock.crear(empresa, sucursal, prenda, CombinacionDeVariante.de(java.util.Map.of(color, rojo)), 9);
		GrupoDeStock azulA = GrupoDeStock.crear(empresa, sucursal, prenda, CombinacionDeVariante.de(java.util.Map.of(color, azul)), 1);

		assertThat(rojoA.mismaVariante(rojoB)).isTrue();
		assertThat(rojoA.mismaVariante(azulA)).isFalse();
	}
}
