package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.TokenDeRefresh;
import com.costumi.backend.identidad.dominio.TokenDeRefreshRepository;
import com.costumi.backend.identidad.dominio.Usuario;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Emite el par de tokens de una sesión y registra el refresco server-side (C2). Cada emisión usa un
 * {@code jti} nuevo dentro de una {@code familiaId}: al iniciar sesión abre una familia nueva; al rotar
 * mantiene la misma familia (para poder revocar toda la cadena si se detecta reuso). Centraliza la lógica
 * que comparten login, registro y refresh.
 */
@Service
class EmisorDeSesion {

	private final EmisorDeTokens emisor;
	private final TokenDeRefreshRepository tokens;

	EmisorDeSesion(EmisorDeTokens emisor, TokenDeRefreshRepository tokens) {
		this.emisor = emisor;
		this.tokens = tokens;
	}

	/** Abre una sesión nueva (login/registro): familia nueva + primer refresco. */
	Credenciales nuevaSesion(Usuario usuario) {
		return emitirEnFamilia(usuario, UUID.randomUUID());
	}

	/** Rota dentro de una familia existente (refresh): nuevo jti, misma cadena. */
	Credenciales rotarEnFamilia(Usuario usuario, UUID familiaId) {
		return emitirEnFamilia(usuario, familiaId);
	}

	private Credenciales emitirEnFamilia(Usuario usuario, UUID familiaId) {
		UUID jti = UUID.randomUUID();
		String access = emisor.emitir(usuario);
		RefreshEmitido refresh = emisor.emitirRefresh(usuario, jti.toString());
		tokens.guardar(TokenDeRefresh.crear(usuario.id(), jti, familiaId, refresh.expiraEn(), Instant.now()));
		return new Credenciales(access, refresh.token());
	}
}
