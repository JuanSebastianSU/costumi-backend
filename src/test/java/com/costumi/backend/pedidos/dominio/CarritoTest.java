package com.costumi.backend.pedidos.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Carrito: puras, sin BD ni Spring. */
class CarritoTest {

	private static Carrito nuevo() {
		return Carrito.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TipoPedido.RENTA);
	}

	@Test
	void un_carrito_nuevo_nace_pendiente_y_vacio() {
		Carrito carrito = nuevo();

		assertThat(carrito.estado()).isEqualTo(EstadoCarrito.PENDIENTE);
		assertThat(carrito.lineas()).isEmpty();
	}

	@Test
	void agregar_la_misma_prenda_suma_cantidad() {
		Carrito carrito = nuevo();
		UUID prenda = UUID.randomUUID();

		carrito.agregarItem(prenda, 2);
		carrito.agregarItem(prenda, 3);

		assertThat(carrito.lineas()).hasSize(1);
		assertThat(carrito.lineas().get(0).cantidad()).isEqualTo(5);
	}

	@Test
	void agregar_prendas_distintas_crea_lineas_distintas() {
		Carrito carrito = nuevo();

		carrito.agregarItem(UUID.randomUUID(), 1);
		carrito.agregarItem(UUID.randomUUID(), 1);

		assertThat(carrito.lineas()).hasSize(2);
	}

	@Test
	void la_cantidad_debe_ser_positiva() {
		Carrito carrito = nuevo();

		assertThatThrownBy(() -> carrito.agregarItem(UUID.randomUUID(), 0))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
