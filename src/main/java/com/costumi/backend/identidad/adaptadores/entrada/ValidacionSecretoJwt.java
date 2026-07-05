package com.costumi.backend.identidad.adaptadores.entrada;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fail-fast de seguridad (cierra deuda de PROGRESS.md): en <b>producción</b> el secreto JWT no
 * puede estar vacío ni ser el default de desarrollo. Si lo es, el arranque falla — así producción
 * nunca corre con el secreto commiteado. En dev/test (sin perfil {@code prod}) el default es válido.
 */
@Component
class ValidacionSecretoJwt {

	static final String SECRETO_DEV_POR_DEFECTO = "dev-secret-cambiar-en-produccion-min-32-bytes-0123456789";

	ValidacionSecretoJwt(@Value("${costumi.security.jwt.secret:}") String secreto, Environment environment) {
		boolean enProduccion = Arrays.asList(environment.getActiveProfiles()).contains("prod");
		if (enProduccion && (secreto == null || secreto.isBlank() || secreto.equals(SECRETO_DEV_POR_DEFECTO))) {
			throw new IllegalStateException(
					"En producción debe configurarse COSTUMI_JWT_SECRET (no usar el secreto de desarrollo).");
		}
	}
}
