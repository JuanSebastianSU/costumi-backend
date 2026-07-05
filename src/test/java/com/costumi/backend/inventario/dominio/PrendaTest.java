package com.costumi.backend.inventario.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Prenda: puras, sin BD ni Spring. */
class PrendaTest {

	private static final UUID EMPRESA = UUID.randomUUID();
	private static final UUID CATEGORIA = UUID.randomUUID();

	@Test
	void crear_una_prenda_de_renta() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Camisa pirata", TipoArticulo.RENTA,
				new BigDecimal("50.00"), null);

		assertThat(prenda.tipoArticulo()).isEqualTo(TipoArticulo.RENTA);
		assertThat(prenda.precioRenta()).isEqualByComparingTo("50.00");
		assertThat(prenda.precioVenta()).isNull();
	}

	@Test
	void una_prenda_de_renta_exige_precio_de_renta() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.RENTA, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void una_prenda_de_ambos_exige_ambos_precios() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.AMBOS,
				new BigDecimal("10.00"), null))
				.isInstanceOf(IllegalArgumentException.class);

		Prenda ok = Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.AMBOS,
				new BigDecimal("10.00"), new BigDecimal("100.00"));
		assertThat(ok.precioRenta()).isEqualByComparingTo("10.00");
		assertThat(ok.precioVenta()).isEqualByComparingTo("100.00");
	}

	@Test
	void el_precio_de_renta_debe_ser_mayor_a_cero() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.RENTA,
				BigDecimal.ZERO, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "  ", TipoArticulo.VENTA,
				null, new BigDecimal("10.00")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
