package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.EstadoEmpresa;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link EmpresaRepository} con JPA. */
@Repository
class EmpresaRepositoryAdapter implements EmpresaRepository {

	private final EmpresaJpaRepository jpa;

	EmpresaRepositoryAdapter(EmpresaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Empresa guardar(Empresa empresa) {
		EmpresaJpaEntity guardada = jpa.save(aEntidad(empresa));
		return aDominio(guardada);
	}

	@Override
	public Optional<Empresa> buscarPorId(UUID id) {
		return jpa.findById(id).map(EmpresaRepositoryAdapter::aDominio);
	}

	@Override
	public List<Empresa> listarPorEstado(EstadoEmpresa estado) {
		return jpa.findByEstado(estado).stream().map(EmpresaRepositoryAdapter::aDominio).toList();
	}

	private static EmpresaJpaEntity aEntidad(Empresa empresa) {
		return new EmpresaJpaEntity(empresa.id(), empresa.nombre(), empresa.estado(), empresa.fechaRegistro(),
				empresa.ubicacion(), empresa.contacto(), empresa.solicitanteId());
	}

	private static Empresa aDominio(EmpresaJpaEntity entidad) {
		return Empresa.rehidratar(entidad.getId(), entidad.getNombre(), entidad.getEstado(), entidad.getFechaRegistro(),
				entidad.getUbicacion(), entidad.getContacto(), entidad.getSolicitanteId());
	}
}
