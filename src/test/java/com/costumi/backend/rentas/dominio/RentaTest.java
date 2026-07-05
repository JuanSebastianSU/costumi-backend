package com.costumi.backend.rentas.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Renta: puras, sin BD ni Spring. */
class RentaTest {

	private static Renta nueva(LocalDate retiro, LocalDate devolucion) {
		return Renta.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				retiro, devolucion, new BigDecimal("20.00"), new BigDecimal("50.00"));
	}

	@Test
	void el_importe_es_precio_por_dias() {
		Renta renta = nueva(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4)); // 3 días

		assertThat(renta.estado()).isEqualTo(EstadoRenta.RESERVADA);
		assertThat(renta.importe()).isEqualByComparingTo("60.00");
	}

	@Test
	void un_solo_dia_cuenta_como_uno() {
		Renta renta = nueva(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1));

		assertThat(renta.importe()).isEqualByComparingTo("20.00");
	}

	@Test
	void la_devolucion_no_puede_ser_antes_del_retiro() {
		assertThatThrownBy(() -> nueva(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 1)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void ciclo_entregar_devolver_cerrar() {
		Renta renta = nueva(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4));

		renta.entregar();
		assertThat(renta.estado()).isEqualTo(EstadoRenta.ACTIVA);
		renta.devolver();
		assertThat(renta.estado()).isEqualTo(EstadoRenta.DEVUELTA);
		renta.cerrar();
		assertThat(renta.estado()).isEqualTo(EstadoRenta.CERRADA);
	}

	@Test
	void no_se_puede_devolver_una_reservada() {
		Renta renta = nueva(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4));

		assertThatThrownBy(renta::devolver).isInstanceOf(TransicionDeRentaInvalida.class);
	}

	@Test
	void una_activa_vencida_se_detecta() {
		Renta renta = nueva(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4));
		renta.entregar();

		assertThat(renta.estaVencida(LocalDate.of(2026, 7, 10))).isTrue();
		assertThat(renta.estaVencida(LocalDate.of(2026, 7, 2))).isFalse();
	}
}
