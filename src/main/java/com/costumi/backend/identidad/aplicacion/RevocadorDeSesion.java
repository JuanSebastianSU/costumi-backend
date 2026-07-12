package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.TokenDeRefreshRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Anula una familia de refrescos en una transacción propia (C2). Se usa cuando se detecta <b>reuso</b>: el
 * flujo de refresco lanza {@link RefreshInvalido} (que provoca rollback), así que la revocación debe
 * confirmarse aparte con {@code REQUIRES_NEW} para no perderse — de lo contrario el token robado seguiría vivo.
 */
@Service
class RevocadorDeSesion {

	private final TokenDeRefreshRepository tokens;

	RevocadorDeSesion(TokenDeRefreshRepository tokens) {
		this.tokens = tokens;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revocarFamilia(UUID familiaId) {
		tokens.revocarFamilia(familiaId);
	}
}
