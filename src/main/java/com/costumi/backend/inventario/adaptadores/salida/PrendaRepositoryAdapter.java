package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link PrendaRepository} con JPA. */
@Repository
class PrendaRepositoryAdapter implements PrendaRepository {

	private final PrendaJpaRepository jpa;

	PrendaRepositoryAdapter(PrendaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Prenda guardar(Prenda prenda) {
		return aDominio(jpa.save(aEntidad(prenda)));
	}

	@Override
	public Optional<Prenda> buscarPorId(UUID id) {
		return jpa.findById(id).map(PrendaRepositoryAdapter::aDominio);
	}

	@Override
	public List<Prenda> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(PrendaRepositoryAdapter::aDominio).toList();
	}

	private static PrendaJpaEntity aEntidad(Prenda p) {
		return new PrendaJpaEntity(p.id(), p.empresaId(), p.categoriaId(), p.nombre(), p.tipoArticulo(),
				p.precioRenta(), p.precioVenta(), p.archivada());
	}

	private static Prenda aDominio(PrendaJpaEntity e) {
		return Prenda.rehidratar(e.getId(), e.getEmpresaId(), e.getCategoriaId(), e.getNombre(), e.getTipoArticulo(),
				e.getPrecioRenta(), e.getPrecioVenta(), e.isArchivada());
	}
}
