package com.costumi.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifica los límites entre módulos del monolito con Spring Modulith (spec §5.3).
 *
 * <p>Un módulo solo puede hablar con otro por su API pública / eventos, nunca por sus
 * clases internas. Esta prueba <b>falla el build</b> si algún módulo cruza un límite
 * indebido. Mientras no haya módulos declarados, {@code verify()} pasa trivialmente.
 */
class ModularityTests {

	static final ApplicationModules MODULES = ApplicationModules.of(BackendApplication.class);

	@Test
	void verificaLimitesDeModulos() {
		MODULES.verify();
	}
}
