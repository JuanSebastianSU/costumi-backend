package com.costumi.backend.inventario.dominio;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la CombinacionDeVariante: puras, sin BD ni Spring. */
class CombinacionDeVarianteTest {

	private static final UUID COLOR = UUID.randomUUID();
	private static final UUID TALLA = UUID.randomUUID();
	private static final UUID ROJO = UUID.randomUUID();
	private static final UUID AZUL = UUID.randomUUID();
	private static final UUID M = UUID.randomUUID();

	@Test
	void dos_combinaciones_con_las_mismas_selecciones_son_iguales_sin_importar_el_orden() {
		Map<UUID, UUID> unaOrden = new LinkedHashMap<>();
		unaOrden.put(COLOR, ROJO);
		unaOrden.put(TALLA, M);

		Map<UUID, UUID> otroOrden = new LinkedHashMap<>();
		otroOrden.put(TALLA, M);
		otroOrden.put(COLOR, ROJO);

		CombinacionDeVariante una = CombinacionDeVariante.de(unaOrden);
		CombinacionDeVariante otra = CombinacionDeVariante.de(otroOrden);

		assertThat(una).isEqualTo(otra);
		assertThat(una.hashCode()).isEqualTo(otra.hashCode());
	}

	@Test
	void distinto_valor_en_una_dimension_es_una_variante_distinta() {
		CombinacionDeVariante rojo = CombinacionDeVariante.de(Map.of(COLOR, ROJO));
		CombinacionDeVariante azul = CombinacionDeVariante.de(Map.of(COLOR, AZUL));

		assertThat(rojo).isNotEqualTo(azul);
	}

	@Test
	void la_variante_unica_esta_vacia_y_es_igual_a_otra_vacia() {
		assertThat(CombinacionDeVariante.unica().esUnica()).isTrue();
		assertThat(CombinacionDeVariante.unica()).isEqualTo(CombinacionDeVariante.de(Map.of()));
	}

	@Test
	void expone_tipos_y_valor_por_dimension() {
		CombinacionDeVariante combinacion = CombinacionDeVariante.de(Map.of(COLOR, ROJO, TALLA, M));

		assertThat(combinacion.tipos()).containsExactlyInAnyOrder(COLOR, TALLA);
		assertThat(combinacion.valorDe(COLOR)).contains(ROJO);
		assertThat(combinacion.valorDe(UUID.randomUUID())).isEmpty();
	}

	@Test
	void rechaza_selecciones_con_tipo_o_valor_nulo() {
		Map<UUID, UUID> conValorNulo = new LinkedHashMap<>();
		conValorNulo.put(COLOR, null);

		assertThatThrownBy(() -> CombinacionDeVariante.de(conValorNulo))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void los_valores_expuestos_son_inmutables() {
		CombinacionDeVariante combinacion = CombinacionDeVariante.de(Map.of(COLOR, ROJO));

		assertThatThrownBy(() -> combinacion.valores().put(TALLA, M))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
