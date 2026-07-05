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
	void un_disfraz_por_partes_tiene_entre_1_y_8_slots() {
		Slot slot = Slot.conPrendaFija(1, "Único", EjeDeTalla.LIBRE, null, PRENDA_FIJA, false);

		assertThatThrownBy(() -> Disfraz.porPartes(EMPRESA, "Vacío", List.of()))
				.isInstanceOf(IllegalArgumentException.class);

		List<Slot> nueve = java.util.stream.IntStream.rangeClosed(1, 9)
				.mapToObj(i -> Slot.conPrendaFija(i, "S" + i, EjeDeTalla.LIBRE, null, UUID.randomUUID(), false))
				.toList();
		assertThatThrownBy(() -> Disfraz.porPartes(EMPRESA, "Demasiados", nueve))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(Disfraz.porPartes(EMPRESA, "OK", List.of(slot)).slots()).hasSize(1);
	}

	@Test
	void unidad_fija_disponible_si_su_prenda_tiene_stock() {
		Disfraz disfraz = Disfraz.unidadFija(EMPRESA, "Traje entero", PRENDA_FIJA);

		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), false))).isTrue();
		assertThat(disfraz.estaDisponible(stock(Set.of(), false))).isFalse();
	}

	@Test
	void por_partes_disponible_si_cada_slot_obligatorio_tiene_stock() {
		Slot obligatorioFijo = Slot.conPrendaFija(1, "Cuerpo", EjeDeTalla.LIBRE, null, PRENDA_FIJA, false);
		Slot obligatorioPool = Slot.personalizable(2, "Sombrero", EjeDeTalla.LIBRE, null,
				PoolDeSlot.de(CATEGORIA, Map.of()), false);
		Disfraz disfraz = Disfraz.porPartes(EMPRESA, "Pirata", List.of(obligatorioFijo, obligatorioPool));

		// El fijo tiene stock, pero el pool no -> no disponible.
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), false))).isFalse();
		// Ambos con stock -> disponible.
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), true))).isTrue();
	}

	@Test
	void los_slots_opcionales_no_bloquean_la_disponibilidad() {
		Slot obligatorio = Slot.conPrendaFija(1, "Cuerpo", EjeDeTalla.LIBRE, null, PRENDA_FIJA, false);
		Slot opcional = Slot.personalizable(2, "Accesorio", EjeDeTalla.LIBRE, null,
				PoolDeSlot.de(CATEGORIA, Map.of()), true);
		Disfraz disfraz = Disfraz.porPartes(EMPRESA, "Con accesorio", List.of(obligatorio, opcional));

		// El opcional no tiene stock (pool=false), pero el obligatorio sí -> disponible.
		assertThat(disfraz.estaDisponible(stock(Set.of(PRENDA_FIJA), false))).isTrue();
	}

	@Test
	void un_slot_de_talla_fija_exige_la_talla() {
		assertThatThrownBy(() -> Slot.conPrendaFija(1, "Cuerpo", EjeDeTalla.FIJA, "  ", PRENDA_FIJA, false))
				.isInstanceOf(IllegalArgumentException.class);

		Slot ok = Slot.conPrendaFija(1, "Cuerpo", EjeDeTalla.FIJA, "M", PRENDA_FIJA, false);
		assertThat(ok.tallaFija()).isEqualTo("M");
	}

	@Test
	void unidad_fija_no_lleva_slots() {
		assertThat(Disfraz.unidadFija(EMPRESA, "Traje", PRENDA_FIJA).slots()).isEmpty();
	}
}
