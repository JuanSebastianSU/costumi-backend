package com.costumi.backend.configuracion.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de dominio de la configuración: puras, sin BD ni Spring. */
class ConfiguracionDeEmpresaTest {

	@Test
	void los_defaults_son_sensatos() {
		ConfiguracionDeEmpresa c = ConfiguracionDeEmpresa.porDefecto(UUID.randomUUID());

		assertThat(c.conteoStock()).isTrue();
		assertThat(c.multasActivo()).isTrue();
		assertThat(c.multiSucursal()).isFalse();
		assertThat(c.pagoEnLinea()).isFalse();
	}
}
