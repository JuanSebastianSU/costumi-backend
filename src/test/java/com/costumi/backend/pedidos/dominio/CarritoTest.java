package com.costumi.backend.pedidos.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Carrito: puras, sin BD ni Spring. */
class CarritoTest {

	/** Carrito de VENTA (sus líneas no llevan fechas): base de las pruebas genéricas de agregado. */
	private static Carrito nuevo() {
		return Carrito.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TipoPedido.VENTA);
	}

	private static Carrito nuevoDeRenta() {
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

	@Test
	void un_articulo_de_renta_requiere_fechas() {
		Carrito carrito = nuevoDeRenta();

		assertThatThrownBy(() -> carrito.agregarItem(UUID.randomUUID(), 1, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void agregar_el_mismo_disfraz_con_la_misma_seleccion_suma_cantidad() {
		Carrito carrito = nuevo();
		UUID disfraz = UUID.randomUUID();
		UUID prendaSlot = UUID.randomUUID();

		carrito.agregarDisfraz(disfraz, List.of(new SeleccionDeSlot(1, prendaSlot)), 1, null, null);
		carrito.agregarDisfraz(disfraz, List.of(new SeleccionDeSlot(1, prendaSlot)), 2, null, null);

		assertThat(carrito.lineas()).hasSize(1);
		assertThat(carrito.lineas().get(0).cantidad()).isEqualTo(3);
		assertThat(carrito.lineas().get(0).esDisfraz()).isTrue();
	}

	@Test
	void el_mismo_disfraz_con_distinta_seleccion_son_lineas_distintas() {
		Carrito carrito = nuevo();
		UUID disfraz = UUID.randomUUID();

		carrito.agregarDisfraz(disfraz, List.of(new SeleccionDeSlot(1, UUID.randomUUID())), 1, null, null);
		carrito.agregarDisfraz(disfraz, List.of(new SeleccionDeSlot(1, UUID.randomUUID())), 1, null, null);

		assertThat(carrito.lineas()).hasSize(2);
	}

	@Test
	void una_prenda_y_un_disfraz_nunca_se_agrupan() {
		Carrito carrito = nuevo();
		UUID id = UUID.randomUUID();

		carrito.agregarItem(id, 1);
		carrito.agregarDisfraz(id, List.of(), 1, null, null);

		assertThat(carrito.lineas()).hasSize(2);
		assertThat(carrito.lineas().get(0).esPrenda()).isTrue();
		assertThat(carrito.lineas().get(1).esDisfraz()).isTrue();
	}

	@Test
	void en_renta_un_disfraz_requiere_fechas() {
		Carrito carrito = nuevoDeRenta();

		assertThatThrownBy(() -> carrito.agregarDisfraz(UUID.randomUUID(), List.of(), 1, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void en_renta_la_misma_prenda_con_distinto_periodo_son_lineas_distintas() {
		Carrito carrito = nuevoDeRenta();
		UUID prenda = UUID.randomUUID();

		carrito.agregarItem(prenda, 1, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-04"));
		carrito.agregarItem(prenda, 2, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-04")); // mismo periodo -> suma
		carrito.agregarItem(prenda, 1, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-03")); // otro periodo -> nueva línea

		assertThat(carrito.lineas()).hasSize(2);
		assertThat(carrito.lineas().get(0).cantidad()).isEqualTo(3);
		assertThat(carrito.lineas().get(1).cantidad()).isEqualTo(1);
	}
}
