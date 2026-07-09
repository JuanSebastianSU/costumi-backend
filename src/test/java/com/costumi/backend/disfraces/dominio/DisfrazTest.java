package com.costumi.backend.disfraces.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Disfraz y su disponibilidad derivada: puras, sin BD ni Spring. */
class DisfrazTest {

	private static final UUID EMPRESA = UUID.randomUUID();
	private static final UUID CATEGORIA = UUID.randomUUID();
	private static final UUID PRENDA_FIJA = UUID.randomUUID();

	/** Stub del puerto: un conjunto de prendas con stock y una respuesta fija para los pools. */
	private static ConsultaDeStockDePool stock(Set<UUID> prendasConStock, boolean poolTieneStock) {
		return new ConsultaDeStockDePool() {
			@Override
			public boolean prendaTieneStock(UUID prendaId) {
				return prendasConStock.contains(prendaId);
			}

			@Override
			public boolean poolTieneStock(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
				return poolTieneStock;
			}
		};
	}

	@Test
	void un_disfraz_tiene_entre_1_y_8_slots() {
		Slot slot = Slot.conPrendaFija(1, "Único", PRENDA_FIJA, false);

		assertThatThrownBy(() -> Disfraz.crear(EMPRESA, "Vacío", List.of()))
				.isInstanceOf(IllegalArgumentException.class);

		List<Slot> nueve = java.util.stream.IntStream.rangeClosed(1, 9)
				.mapToObj(i -> Slot.conPrendaFija(i, "S" + i, UUID.randomUUID(), false))
				.toList();
		assertThatThrownBy(() -> Disfraz.crear(EMPRESA, "Demasiados", nueve))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(Disfraz.crear(EMPRESA, "OK", List.of(slot)).slots()).hasSize(1);
	}

	@Test
	void una_pieza_es_un_disfraz_con_un_unico_slot_fijo() {
		Disfraz disfraz = Disfraz.crear(EMPRESA, "Traje entero",
				List.of(Slot.conPrendaFija(1, "Traje", PRENDA_FIJA, false)));

		assertThat(disfraz.slots()).hasSize(1);
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), false))).isTrue();
		assertThat(disfraz.estaDisponible(stock(Set.of(), false))).isFalse();
	}

	@Test
	void por_partes_disponible_si_cada_slot_obligatorio_tiene_stock() {
		Slot obligatorioFijo = Slot.conPrendaFija(1, "Cuerpo", PRENDA_FIJA, false);
		Slot obligatorioPool = Slot.personalizable(2, "Sombrero", PoolDeSlot.de(CATEGORIA, Map.of()), false);
		Disfraz disfraz = Disfraz.crear(EMPRESA, "Pirata", List.of(obligatorioFijo, obligatorioPool));

		// El fijo tiene stock, pero el pool no -> no disponible.
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), false))).isFalse();
		// Ambos con stock -> disponible.
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), true))).isTrue();
	}

	@Test
	void los_slots_opcionales_no_bloquean_la_disponibilidad() {
		Slot obligatorio = Slot.conPrendaFija(1, "Cuerpo", PRENDA_FIJA, false);
		Slot opcional = Slot.personalizable(2, "Accesorio", PoolDeSlot.de(CATEGORIA, Map.of()), true);
		Disfraz disfraz = Disfraz.crear(EMPRESA, "Con accesorio", List.of(obligatorio, opcional));

		// El opcional no tiene stock (pool=false), pero el obligatorio sí -> disponible.
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), false))).isTrue();
	}

	@Test
	void un_slot_personalizable_exige_pool_y_uno_fijo_exige_prenda() {
		assertThatThrownBy(() -> Slot.personalizable(1, "X", null, false))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Slot.conPrendaFija(1, "X", null, false))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void archivar_y_activar_cambia_el_estado() {
		Disfraz disfraz = Disfraz.crear(EMPRESA, "Traje", List.of(Slot.conPrendaFija(1, "Traje", PRENDA_FIJA, false)));
		assertThat(disfraz.activo()).isTrue();

		disfraz.archivar();
		assertThat(disfraz.activo()).isFalse();

		disfraz.activar();
		assertThat(disfraz.activo()).isTrue();
	}

	@Test
	void redefinir_reemplaza_nombre_y_slots() {
		Disfraz disfraz = Disfraz.crear(EMPRESA, "Viejo", List.of(Slot.conPrendaFija(1, "A", PRENDA_FIJA, false)));

		UUID otra = UUID.randomUUID();
		disfraz.redefinir("Nuevo", List.of(
				Slot.conPrendaFija(1, "A", PRENDA_FIJA, false),
				Slot.conPrendaFija(2, "B", otra, false)), null);

		assertThat(disfraz.nombre()).isEqualTo("Nuevo");
		assertThat(disfraz.slots()).hasSize(2);
	}

	@Test
	void precio_general_es_opcional_y_no_puede_ser_negativo() {
		Slot slot = Slot.conPrendaFija(1, "Traje", PRENDA_FIJA, false);

		Disfraz porPrendas = Disfraz.crear(EMPRESA, "Sin general", List.of(slot));
		assertThat(porPrendas.tienePrecioGeneral()).isFalse();

		Disfraz conGeneral = Disfraz.crear(EMPRESA, "Con general", List.of(slot), new java.math.BigDecimal("120.00"));
		assertThat(conGeneral.tienePrecioGeneral()).isTrue();
		assertThat(conGeneral.precioRentaGeneral()).isEqualByComparingTo("120.00");

		assertThatThrownBy(() -> Disfraz.crear(EMPRESA, "Negativo", List.of(slot), new java.math.BigDecimal("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
