package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.clientes.dominio.Favorito;
import com.costumi.backend.clientes.dominio.FavoritoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link FavoritoRepository} con JPA. */
@Repository
class FavoritoRepositoryAdapter implements FavoritoRepository {

	private final FavoritoJpaRepository jpa;

	FavoritoRepositoryAdapter(FavoritoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public List<Favorito> listarPorUsuario(UUID usuarioId) {
		return jpa.findByUsuarioIdOrderByGuardadoEnDesc(usuarioId).stream()
				.map(FavoritoRepositoryAdapter::aDominio).toList();
	}

	@Override
	public Optional<Favorito> buscar(UUID usuarioId, UUID disfrazId) {
		return jpa.findByUsuarioIdAndDisfrazId(usuarioId, disfrazId).map(FavoritoRepositoryAdapter::aDominio);
	}

	@Override
	public Favorito guardar(Favorito favorito) {
		return aDominio(jpa.save(aEntidad(favorito)));
	}

	@Override
	public void eliminar(UUID usuarioId, UUID disfrazId) {
		jpa.deleteByUsuarioIdAndDisfrazId(usuarioId, disfrazId);
	}

	private static FavoritoJpaEntity aEntidad(Favorito f) {
		return new FavoritoJpaEntity(f.id(), f.usuarioId(), f.disfrazId(), f.empresaId(), f.nombre(), f.fotoUrl(),
				f.precioRenta(), f.precioVenta(), f.guardadoEn());
	}

	private static Favorito aDominio(FavoritoJpaEntity e) {
		return Favorito.rehidratar(e.getId(), e.getUsuarioId(), e.getDisfrazId(), e.getEmpresaId(), e.getNombre(),
				e.getFotoUrl(), e.getPrecioRenta(), e.getPrecioVenta(), e.getGuardadoEn());
	}
}
