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
	}

	@Test
	void la_tasa_de_impuesto_debe_estar_entre_0_y_1() {
		UUID empresa = UUID.randomUUID();
		// Válida: 19%.
		assertThat(ConfiguracionDeEmpresa.de(empresa, true, true, false, false, new BigDecimal("0.19")).tasaImpuesto())
				.isEqualByComparingTo("0.19");
		// Inválidas: negativa o >= 1 (100%).
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, new BigDecimal("-0.1")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ConfiguracionDeEmpresa.de(empresa, true, true, false, false, BigDecimal.ONE))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
