package com.costumi.backend.ventas.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Venta: puras, sin BD ni Spring. */
class VentaTest {

	private static Venta ventaCon(BigDecimal descuento, LineaDeVenta... lineas) {
		return Venta.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, descuento, List.of(lineas),
				null);
	}

	@Test
	void el_total_es_la_suma_menos_el_descuento() {
		Venta venta = ventaCon(new BigDecimal("10.00"),
				LineaDeVenta.de(UUID.randomUUID(), 2, new BigDecimal("50.00")),
				LineaDeVenta.de(UUID.randomUUID(), 1, new BigDecimal("30.00")));

		assertThat(venta.total()).isEqualByComparingTo("120.00"); // 100 + 30 - 10
		assertThat(venta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
		assertThat(venta.lineas()).hasSize(2);
	}

	@Test
	void una_venta_sin_lineas_es_invalida() {
		assertThatThrownBy(() -> ventaCon(BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void el_descuento_no_puede_exceder_el_subtotal() {
		assertThatThrownBy(() -> ventaCon(new BigDecimal("200.00"),
				LineaDeVenta.de(UUID.randomUUID(), 1, new BigDecimal("50.00"))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void la_cantidad_y_el_precio_deben_ser_positivos() {
		assertThatThrownBy(() -> LineaDeVenta.de(UUID.randomUUID(), 0, new BigDecimal("10.00")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> LineaDeVenta.de(UUID.randomUUID(), 1, BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
