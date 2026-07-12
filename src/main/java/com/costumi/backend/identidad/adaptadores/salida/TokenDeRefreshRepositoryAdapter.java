package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoDeRefresh;
import com.costumi.backend.identidad.dominio.TokenDeRefresh;
import com.costumi.backend.identidad.dominio.TokenDeRefreshRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link TokenDeRefreshRepository} con JPA. */
@Repository
class TokenDeRefreshRepositoryAdapter implements TokenDeRefreshRepository {

	private final TokenDeRefreshJpaRepository jpa;

	TokenDeRefreshRepositoryAdapter(TokenDeRefreshJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public TokenDeRefresh guardar(TokenDeRefresh token) {
		return aDominio(jpa.save(aEntidad(token)));
	}

	@Override
	public Optional<TokenDeRefresh> buscarPorJti(UUID jti) {
		return jpa.findFirstByJti(jti).map(TokenDeRefreshRepositoryAdapter::aDominio);
	}

	@Override
	public void revocarFamilia(UUID familiaId) {
		jpa.revocarFamilia(familiaId, EstadoDeRefresh.REVOCADO);
	}

	private static TokenDeRefreshJpaEntity aEntidad(TokenDeRefresh t) {
		return new TokenDeRefreshJpaEntity(t.jti(), t.usuarioId(), t.familiaId(), t.estado(), t.expiraEn(),
				t.creadoEn());
	}

	private static TokenDeRefresh aDominio(TokenDeRefreshJpaEntity e) {
		return TokenDeRefresh.rehidratar(e.getJti(), e.getUsuarioId(), e.getFamiliaId(), e.getEstado(),
				e.getExpiraEn(), e.getCreadoEn());
	}
}
