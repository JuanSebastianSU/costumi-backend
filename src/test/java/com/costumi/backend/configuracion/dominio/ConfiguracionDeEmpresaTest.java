package com.costumi.backend.configuracion.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la configuración: puras, sin BD ni Spring. */
class ConfiguracionDeEmpresaTest {

	@Test
	void los_defaults_son_sensatos() {
		ConfiguracionDeEmpresa c = ConfiguracionDeEmpresa.porDefecto(UUID.randomUUID());

		assertThat(c.conteoStock()).isTrue();
		assertThat(c.multasActivo()).isTrue();
		assertThat(c.multiSucursal()).isFalse();
		assertThat(c.pagoEnLinea()).isFalse();
		assertThat(c.tasaImpuesto()).isEqualByComparingTo("0"); // sin impuesto por defecto (RF-6.5)
		assertThat(c.moneda()).isEqualTo("COP"); // moneda por defecto (RF-12.2)
		assertThat(c.recargoPorRetrasoPorDia()).isEqualByComparingTo("0");
		assertThat(c.modoRecargoRetraso()).isEqualTo(RecargoPorRetraso.ACUMULATIVA); // recargo acumulativo por defecto
	}

	@Test
	void la_tasa_de_impuesto_debe_estar_entre_0_y_1() {
		UUID empresa = UUID.randomUUID();
		// Válida: 19%.
		assertThat(ConfiguracionDeEmpresa
				.de(empresa, true, true, false, false, new BigDecimal("0.19"), "COP", BigDecimal.ZERO, null)
				.tasaImpuesto())
				.isEqualByComparingTo("0.19");
		// Inválidas: negativa o >= 1 (100%).
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, new BigDecimal("-0.1"),
				"COP", BigDecimal.ZERO, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ONE, "COP",
				BigDecimal.ZERO, null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void las_reglas_por_defecto_se_pueden_configurar_y_se_validan() {
		UUID empresa = UUID.randomUUID();
		ConfiguracionDeEmpresa c = ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ZERO, "usd",
				new BigDecimal("5.00"), null);
		assertThat(c.moneda()).isEqualTo("USD"); // se normaliza a mayúsculas
		assertThat(c.recargoPorRetrasoPorDia()).isEqualByComparingTo("5.00");
		// Moneda en blanco o recargo negativo -> inválido.
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ZERO, "  ",
				BigDecimal.ZERO, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ZERO, "COP",
				new BigDecimal("-1"), null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void el_recargo_acumulativo_multiplica_por_dias_y_el_fijo_es_monto_unico() {
		UUID empresa = UUID.randomUUID();
		ConfiguracionDeEmpresa acumulativa = ConfiguracionDeEmpresa.de(empresa, true, true, false, false,
				BigDecimal.ZERO, "COP", new BigDecimal("10.00"), RecargoPorRetraso.ACUMULATIVA);
		ConfiguracionDeEmpresa fija = ConfiguracionDeEmpresa.de(empresa, true, true, false, false,
				BigDecimal.ZERO, "COP", new BigDecimal("10.00"), RecargoPorRetraso.FIJA);

		// Sin atraso, ninguna cobra.
		assertThat(acumulativa.recargoPorRetraso(0)).isEqualByComparingTo("0");
		assertThat(fija.recargoPorRetraso(0)).isEqualByComparingTo("0");
		// 3 días de atraso: acumulativa = 10 × 3 = 30; fija = 10 (monto único).
		assertThat(acumulativa.recargoPorRetraso(3)).isEqualByComparingTo("30.00");
		assertThat(fija.recargoPorRetraso(3)).isEqualByComparingTo("10.00");
	}
}
