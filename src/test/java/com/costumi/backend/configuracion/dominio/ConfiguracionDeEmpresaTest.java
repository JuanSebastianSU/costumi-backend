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
	}

	@Test
	void la_tasa_de_impuesto_debe_estar_entre_0_y_1() {
		UUID empresa = UUID.randomUUID();
		// Válida: 19%.
		assertThat(ConfiguracionDeEmpresa
				.de(empresa, true, true, false, false, new BigDecimal("0.19"), "COP", BigDecimal.ZERO).tasaImpuesto())
				.isEqualByComparingTo("0.19");
		// Inválidas: negativa o >= 1 (100%).
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, new BigDecimal("-0.1"),
				"COP", BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ONE, "COP",
				BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void las_reglas_por_defecto_se_pueden_configurar_y_se_validan() {
		UUID empresa = UUID.randomUUID();
		ConfiguracionDeEmpresa c = ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ZERO, "usd",
				new BigDecimal("5.00"));
		assertThat(c.moneda()).isEqualTo("USD"); // se normaliza a mayúsculas
		assertThat(c.recargoPorRetrasoPorDia()).isEqualByComparingTo("5.00");
		// Moneda en blanco o recargo negativo -> inválido.
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ZERO, "  ",
				BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ZERO, "COP",
				new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
	}
}
