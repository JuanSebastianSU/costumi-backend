package com.costumi.backend.auditoria.adaptadores.salida;

import com.costumi.backend.auditoria.dominio.RegistroDeAuditoria;
import com.costumi.backend.auditoria.dominio.RegistroDeAuditoriaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link RegistroDeAuditoriaRepository} con JPA. */
@Repository
class RegistroDeAuditoriaRepositoryAdapter implements RegistroDeAuditoriaRepository {

	private final RegistroDeAuditoriaJpaRepository jpa;

	RegistroDeAuditoriaRepositoryAdapter(RegistroDeAuditoriaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public RegistroDeAuditoria guardar(RegistroDeAuditoria registro) {
		return aDominio(jpa.save(new RegistroDeAuditoriaJpaEntity(registro.id(), registro.empresaId(),
				registro.accion(), registro.detalle(), registro.fecha())));
	}

	@Override
	public List<RegistroDeAuditoria> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaIdOrderByFechaDesc(empresaId).stream()
				.map(RegistroDeAuditoriaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public com.costumi.backend.compartido.Pagina<RegistroDeAuditoria> listarPorEmpresa(UUID empresaId, String buscar,
			com.costumi.backend.compartido.SolicitudDePagina pagina) {
		org.springframework.data.domain.Page<RegistroDeAuditoriaJpaEntity> page = jpa.buscarPagina(empresaId,
				buscar == null || buscar.isBlank() ? null : buscar.trim(),
				org.springframework.data.domain.PageRequest.of(pagina.pagina(), pagina.tamano()));
		return com.costumi.backend.compartido.Pagina.de(page.getContent().stream()
				.map(RegistroDeAuditoriaRepositoryAdapter::aDominio).toList(), page.getTotalElements(), pagina);
	}

	private static RegistroDeAuditoria aDominio(RegistroDeAuditoriaJpaEntity e) {
		return RegistroDeAuditoria.rehidratar(e.getId(), e.getEmpresaId(), e.getAccion(), e.getDetalle(), e.getFecha());
	}
}
