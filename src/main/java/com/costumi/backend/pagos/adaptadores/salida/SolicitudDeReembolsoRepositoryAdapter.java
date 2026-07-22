package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.EstadoSolicitudReembolso;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolsoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link SolicitudDeReembolsoRepository} con JPA. */
@Repository
class SolicitudDeReembolsoRepositoryAdapter implements SolicitudDeReembolsoRepository {

	private final SolicitudDeReembolsoJpaRepository jpa;

	SolicitudDeReembolsoRepositoryAdapter(SolicitudDeReembolsoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public SolicitudDeReembolso guardar(SolicitudDeReembolso solicitud) {
		return aDominio(jpa.save(aEntidad(solicitud)));
	}

	@Override
	public Optional<SolicitudDeReembolso> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(SolicitudDeReembolsoRepositoryAdapter::aDominio);
	}

	@Override
	public List<SolicitudDeReembolso> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaIdOrderByCreadaEnDesc(empresaId).stream()
				.map(SolicitudDeReembolsoRepositoryAdapter::aDominio).toList();
	}

	@Override
	public com.costumi.backend.compartido.Pagina<SolicitudDeReembolso> listarPorEmpresa(UUID empresaId, String buscar,
			com.costumi.backend.compartido.SolicitudDePagina pagina) {
		org.springframework.data.domain.Page<SolicitudDeReembolsoJpaEntity> page = jpa.buscarPagina(empresaId,
				buscar == null || buscar.isBlank() ? null : buscar.trim(),
				org.springframework.data.domain.PageRequest.of(pagina.pagina(), pagina.tamano()));
		return com.costumi.backend.compartido.Pagina.de(page.getContent().stream()
				.map(SolicitudDeReembolsoRepositoryAdapter::aDominio).toList(), page.getTotalElements(), pagina);
	}

	@Override
	public boolean existePendientePorConcepto(UUID empresaId, UUID conceptoId) {
		return jpa.existsByEmpresaIdAndConceptoIdAndEstado(empresaId, conceptoId,
				EstadoSolicitudReembolso.PENDIENTE);
	}

	private static SolicitudDeReembolsoJpaEntity aEntidad(SolicitudDeReembolso s) {
		return new SolicitudDeReembolsoJpaEntity(s.id(), s.empresaId(), s.tipoConcepto(), s.conceptoId(),
				s.solicitanteClienteId(), s.monto(), s.motivoSolicitud(), s.estado(), s.motivoDecision(),
				s.decididoPorUsuarioId(), s.rolDecision(), s.creadaEn(), s.decididaEn());
	}

	private static SolicitudDeReembolso aDominio(SolicitudDeReembolsoJpaEntity e) {
		return SolicitudDeReembolso.rehidratar(e.getId(), e.getEmpresaId(), e.getTipoConcepto(), e.getConceptoId(),
				e.getSolicitanteClienteId(), e.getMonto(), e.getMotivoSolicitud(), e.getEstado(),
				e.getMotivoDecision(), e.getDecididoPorUsuarioId(), e.getRolDecision(), e.getCreadaEn(),
				e.getDecididaEn());
	}
}
