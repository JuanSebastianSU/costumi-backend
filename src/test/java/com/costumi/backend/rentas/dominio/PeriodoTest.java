package com.costumi.backend.rentas.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Periodo de renta y su lógica de traslape: puras, sin BD ni Spring. */
class PeriodoTest {

	private static Periodo p(String retiro, String devolucion) {
		return new Periodo(LocalDate.parse(retiro), LocalDate.parse(devolucion));
	}

	@Test
	void periodos_que_se_cruzan_se_solapan() {
		assertThat(p("2026-07-01", "2026-07-05").seSolapaCon(p("2026-07-04", "2026-07-08"))).isTrue();
	}

	@Test
	void periodos_disjuntos_no_se_solapan() {
		assertThat(p("2026-07-01", "2026-07-05").seSolapaCon(p("2026-07-06", "2026-07-08"))).isFalse();
	}

	@Test
	void compartir_un_extremo_cuenta_como_traslape() {
		assertThat(p("2026-07-01", "2026-07-05").seSolapaCon(p("2026-07-05", "2026-07-09"))).isTrue();
	}

	@Test
	void uno_contenido_en_otro_se_solapa() {
		assertThat(p("2026-07-01", "2026-07-10").seSolapaCon(p("2026-07-03", "2026-07-04"))).isTrue();
	}

	@Test
	void la_devolucion_no_puede_ser_anterior_al_retiro() {
		assertThatThrownBy(() -> p("2026-07-05", "2026-07-01")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void el_periodo_minimo_es_un_dia() {
		assertThat(p("2026-07-01", "2026-07-01").dias()).isEqualTo(1);
		assertThat(p("2026-07-01", "2026-07-04").dias()).isEqualTo(3);
	}
}
