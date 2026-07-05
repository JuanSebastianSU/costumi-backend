package com.costumi.backend.identidad.adaptadores.entrada;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica el fail-fast del secreto JWT sin arrancar Spring (usa un Environment de prueba). */
class ValidacionSecretoJwtTest {

	private static MockEnvironment perfilProd() {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles("prod");
		return env;
	}

	@Test
	void falla_en_produccion_con_el_secreto_por_defecto() {
		assertThatThrownBy(() -> new ValidacionSecretoJwt(ValidacionSecretoJwt.SECRETO_DEV_POR_DEFECTO, perfilProd()))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void falla_en_produccion_si_el_secreto_esta_vacio() {
		assertThatThrownBy(() -> new ValidacionSecretoJwt("", perfilProd()))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void en_produccion_con_un_secreto_propio_no_falla() {
		assertThatCode(() -> new ValidacionSecretoJwt(
				"un-secreto-propio-suficientemente-largo-de-mas-de-32-bytes", perfilProd()))
				.doesNotThrowAnyException();
	}

	@Test
	void en_desarrollo_el_secreto_por_defecto_es_aceptable() {
		assertThatCode(() -> new ValidacionSecretoJwt(ValidacionSecretoJwt.SECRETO_DEV_POR_DEFECTO, new MockEnvironment()))
				.doesNotThrowAnyException();
	}
}
