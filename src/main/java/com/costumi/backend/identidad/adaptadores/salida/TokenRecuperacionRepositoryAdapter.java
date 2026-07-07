package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.TokenDeRecuperacion;
import com.costumi.backend.identidad.dominio.TokenRecuperacionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador de salida: implementa el puerto {@link TokenRecuperacionRepository} con JPA. */
@Repository
class TokenRecuperacionRepositoryAdapter implements TokenRecuperacionRepository {

	private final TokenRecuperacionJpaRepository jpa;

	TokenRecuperacionRepositoryAdapter(TokenRecuperacionJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public TokenDeRecuperacion guardar(TokenDeRecuperacion token) {
		return aDominio(jpa.save(aEntidad(token)));
	}

	@Override
	public Optional<TokenDeRecuperacion> buscarPorHash(String tokenHash) {
		return jpa.findFirstByTokenHash(tokenHash).map(TokenRecuperacionRepositoryAdapter::aDominio);
	}

	private static TokenRecuperacionJpaEntity aEntidad(TokenDeRecuperacion t) {
		return new TokenRecuperacionJpaEntity(t.id(), t.usuarioId(), t.tokenHash(), t.expiraEn(), t.usado());
	}

	private static TokenDeRecuperacion aDominio(TokenRecuperacionJpaEntity e) {
		return TokenDeRecuperacion.rehidratar(e.getId(), e.getUsuarioId(), e.getTokenHash(), e.getExpiraEn(),
				e.isUsado());
	}
}
