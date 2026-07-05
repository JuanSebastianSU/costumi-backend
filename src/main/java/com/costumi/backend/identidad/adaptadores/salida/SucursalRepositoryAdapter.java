package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link SucursalRepository} con JPA. */
@Repository
class SucursalRepositoryAdapter implements SucursalRepository {

	private final SucursalJpaRepository jpa;

	SucursalRepositoryAdapter(SucursalJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Sucursal guardar(Sucursal sucursal) {
		return aDominio(jpa.save(aEntidad(sucursal)));
	}

	@Override
	public Optional<Sucursal> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(SucursalRepositoryAdapter::aDominio);
	}

	@Override
	public List<Sucursal> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(SucursalRepositoryAdapter::aDominio).toList();
	}

	private static SucursalJpaEntity aEntidad(Sucursal s) {
		return new SucursalJpaEntity(s.id(), s.empresaId(), s.nombre(), s.direccion());
	}

	private static Sucursal aDominio(SucursalJpaEntity e) {
		return Sucursal.rehidratar(e.getId(), e.getEmpresaId(), e.getNombre(), e.getDireccion());
	}
}
