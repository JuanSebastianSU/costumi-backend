package com.costumi.backend.reportes.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de dominio del resumen de ingresos: puras, sin BD ni Spring. */
class ResumenDeIngresosTest {

	@Test
	void el_total_suma_renta_y_venta() {
		ResumenDeIngresos resumen = ResumenDeIngresos.de(new BigDecimal("40.00"), new BigDecimal("60.00"));

		assertThat(resumen.total()).isEqualByComparingTo("100.00");
	}

	@Test
	void los_nulos_cuentan_como_cero() {
		ResumenDeIngresos resumen = ResumenDeIngresos.de(null, null);

		assertThat(resumen.total()).isEqualByComparingTo("0");
	}
}
