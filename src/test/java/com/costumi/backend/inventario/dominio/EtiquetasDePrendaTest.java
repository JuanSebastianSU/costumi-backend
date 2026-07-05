package com.costumi.backend.inventario.dominio;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de EtiquetasDePrenda: puras, sin BD ni Spring. */
class EtiquetasDePrendaTest {

	private static final UUID COLOR = UUID.randomUUID();
	private static final UUID TEMA = UUID.randomUUID();
	private static final UUID ROJO = UUID.randomUUID();
	private static final UUID SUPERHEROE = UUID.randomUUID();

	@Test
	void clasifica_por_dimension_y_expone_los_tipos() {
		EtiquetasDePrenda etiquetas = EtiquetasDePrenda.de(Map.of(COLOR, ROJO, TEMA, SUPERHEROE));

		assertThat(etiquetas.esVacia()).isFalse();
		assertThat(etiquetas.tipos()).containsExactlyInAnyOrder(COLOR, TEMA);
		assertThat(etiquetas.valorDe(COLOR)).contains(ROJO);
		assertThat(etiquetas.valorDe(UUID.randomUUID())).isEmpty();
	}

	@Test
	void la_prenda_sin_etiquetas_esta_vacia() {
		assertThat(EtiquetasDePrenda.ninguna().esVacia()).isTrue();
		assertThat(EtiquetasDePrenda.ninguna()).isEqualTo(EtiquetasDePrenda.de(Map.of()));
	}

	@Test
	void rechaza_valores_nulos() {
		Map<UUID, UUID> conNulo = new LinkedHashMap<>();
		conNulo.put(COLOR, null);

		assertThatThrownBy(() -> EtiquetasDePrenda.de(conNulo)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void los_valores_expuestos_son_inmutables() {
		EtiquetasDePrenda etiquetas = EtiquetasDePrenda.de(Map.of(COLOR, ROJO));

		assertThatThrownBy(() -> etiquetas.valores().put(TEMA, SUPERHEROE))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
