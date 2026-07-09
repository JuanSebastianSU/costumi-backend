package com.costumi.backend.ventas.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

	@Test
	void devolucion_parcial_marca_parcial_y_luego_devuelta() {
		UUID prenda = UUID.randomUUID();
		Venta venta = ventaCon(BigDecimal.ZERO, LineaDeVenta.de(prenda, 3, new BigDecimal("50.00")));

		// Devuelve 1 de 3 -> PARCIALMENTE_DEVUELTA.
		Map<UUID, Integer> devueltas = venta.devolver(Map.of(prenda, 1));
		assertThat(devueltas).containsEntry(prenda, 1);
		assertThat(venta.estado()).isEqualTo(EstadoVenta.PARCIALMENTE_DEVUELTA);
		assertThat(venta.lineas().get(0).cantidadDevuelta()).isEqualTo(1);

		// Devuelve el resto (null = todo lo pendiente, 2 unidades) -> DEVUELTA.
		Map<UUID, Integer> resto = venta.devolver(null);
		assertThat(resto).containsEntry(prenda, 2);
		assertThat(venta.estado()).isEqualTo(EstadoVenta.DEVUELTA);

		// Ya no queda nada pendiente por devolver.
		assertThatThrownBy(() -> venta.devolver(null)).isInstanceOf(VentaNoDevolvible.class);
	}

	@Test
	void no_se_puede_devolver_mas_de_lo_pendiente_ni_una_prenda_ajena() {
		UUID prenda = UUID.randomUUID();
		Venta venta = ventaCon(BigDecimal.ZERO, LineaDeVenta.de(prenda, 2, new BigDecimal("50.00")));

		assertThatThrownBy(() -> venta.devolver(Map.of(prenda, 3)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> venta.devolver(Map.of(UUID.randomUUID(), 1)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
