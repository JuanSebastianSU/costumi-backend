package com.costumi.backend.devoluciones.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Devolución: puras, sin BD ni Spring. */
class DevolucionTest {

	private static final UUID EMPRESA = UUID.randomUUID();
	private static final UUID RENTA = UUID.randomUUID();
	private static final UUID PRENDA = UUID.randomUUID();

	@Test
	void liquidacion_resta_danos_y_retraso_del_deposito() {
		Devolucion dev = Devolucion.crear(EMPRESA, RENTA, new BigDecimal("100.00"),
				new BigDecimal("30.00"), new BigDecimal("10.00"),
				List.of(PiezaRevisada.de(PRENDA, "Camisa", true, EstadoPieza.DANADA)));

		assertThat(dev.remanente()).isEqualByComparingTo("60.00");
		assertThat(dev.piezas()).hasSize(1);
	}

	@Test
	void el_remanente_no_baja_de_cero() {
		Devolucion dev = Devolucion.crear(EMPRESA, RENTA, new BigDecimal("50.00"),
				new BigDecimal("80.00"), BigDecimal.ZERO,
				List.of(PiezaRevisada.de(PRENDA, "Pantalón", true, EstadoPieza.PERDIDA)));

		assertThat(dev.remanente()).isEqualByComparingTo("0");
	}

	@Test
	void la_multa_es_el_exceso_de_cargos_sobre_el_deposito() {
		// Cargos 80 + 20 = 100 > depósito 50 -> multa 50, remanente 0.
		Devolucion conMulta = Devolucion.crear(EMPRESA, RENTA, new BigDecimal("50.00"),
				new BigDecimal("80.00"), new BigDecimal("20.00"),
				List.of(PiezaRevisada.de(PRENDA, "Pantalón", true, EstadoPieza.PERDIDA)));
		assertThat(conMulta.multa()).isEqualByComparingTo("50.00");
		assertThat(conMulta.remanente()).isEqualByComparingTo("0");

		// Depósito cubre los cargos -> multa 0.
		Devolucion sinMulta = Devolucion.crear(EMPRESA, RENTA, new BigDecimal("100.00"),
				new BigDecimal("30.00"), new BigDecimal("10.00"),
				List.of(PiezaRevisada.de(PRENDA, "Camisa", true, EstadoPieza.DANADA)));
		assertThat(sinMulta.multa()).isEqualByComparingTo("0");
	}

	@Test
	void los_cargos_no_pueden_ser_negativos() {
		assertThatThrownBy(() -> Devolucion.crear(EMPRESA, RENTA, new BigDecimal("50.00"),
				new BigDecimal("-1.00"), BigDecimal.ZERO, List.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void la_descripcion_de_la_pieza_es_obligatoria() {
		assertThatThrownBy(() -> PiezaRevisada.de(PRENDA, "  ", true, EstadoPieza.BIEN))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
