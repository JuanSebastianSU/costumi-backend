package com.costumi.backend.caja.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Turno de caja: corte por método y cuadre de efectivo. Puras, sin BD ni Spring. */
class TurnoTest {

	private static Turno abierto(String fondo) {
		return Turno.abrir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(fondo));
	}

	private static MovimientoDeCaja mov(TipoMovimiento tipo, String monto, MetodoDePago metodo) {
		return MovimientoDeCaja.de(tipo, "concepto", new BigDecimal(monto), metodo);
	}

	@Test
	void el_corte_suma_por_metodo_e_incluye_el_fondo_en_efectivo() {
		Turno turno = abierto("100.00");
		turno.registrar(mov(TipoMovimiento.INGRESO, "50.00", MetodoDePago.EFECTIVO));
		turno.registrar(mov(TipoMovimiento.EGRESO, "20.00", MetodoDePago.EFECTIVO));
		turno.registrar(mov(TipoMovimiento.INGRESO, "80.00", MetodoDePago.TARJETA));

		// Efectivo: 100 + 50 − 20 = 130 ; Tarjeta: 80 ; Transferencia: 0.
		assertThat(turno.totalPorMetodo(MetodoDePago.EFECTIVO)).isEqualByComparingTo("130.00");
		assertThat(turno.totalPorMetodo(MetodoDePago.TARJETA)).isEqualByComparingTo("80.00");
		assertThat(turno.totalPorMetodo(MetodoDePago.TRANSFERENCIA)).isEqualByComparingTo("0");
	}

	@Test
	void el_cuadre_es_la_diferencia_entre_contado_y_esperado() {
		Turno turno = abierto("100.00");
		turno.registrar(mov(TipoMovimiento.INGRESO, "50.00", MetodoDePago.EFECTIVO));

		turno.cerrar(new BigDecimal("145.00")); // esperado 150 -> falta 5
		assertThat(turno.estado()).isEqualTo(EstadoTurno.CERRADO);
		assertThat(turno.diferenciaDeEfectivo()).isEqualByComparingTo("-5.00");
	}

	@Test
	void no_se_puede_registrar_ni_cerrar_un_turno_cerrado() {
		Turno turno = abierto("0.00");
		turno.cerrar(new BigDecimal("0.00"));

		assertThatThrownBy(() -> turno.registrar(mov(TipoMovimiento.INGRESO, "10.00", MetodoDePago.EFECTIVO)))
				.isInstanceOf(TurnoNoAbierto.class);
		assertThatThrownBy(() -> turno.cerrar(new BigDecimal("0.00"))).isInstanceOf(TurnoNoAbierto.class);
	}

	@Test
	void el_fondo_inicial_no_puede_ser_negativo() {
		assertThatThrownBy(() -> abierto("-1.00")).isInstanceOf(IllegalArgumentException.class);
	}
}
