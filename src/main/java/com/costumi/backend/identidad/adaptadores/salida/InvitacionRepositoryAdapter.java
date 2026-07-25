package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoInvitacion;
import com.costumi.backend.identidad.dominio.Invitacion;
import com.costumi.backend.identidad.dominio.InvitacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa {@link InvitacionRepository} con JPA. */
@Repository
class InvitacionRepositoryAdapter implements InvitacionRepository {

	private final InvitacionJpaRepository jpa;

	InvitacionRepositoryAdapter(InvitacionJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Invitacion guardar(Invitacion i) {
		return aDominio(jpa.save(new InvitacionJpaEntity(i.id(), i.empresaId(), i.email(), i.rol(),
				i.sucursalIds(), i.tokenHash(), i.expiraEn(), i.estado(), i.aceptoTerminosEn())));
	}

	@Override
	public Optional<Invitacion> buscarPorHash(String tokenHash) {
		return jpa.findFirstByTokenHash(tokenHash).map(InvitacionRepositoryAdapter::aDominio);
	}

	@Override
	public Optional<Invitacion> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(InvitacionRepositoryAdapter::aDominio);
	}

	@Override
	public List<Invitacion> pendientesDeEmpresa(UUID empresaId) {
		return jpa.findByEmpresaIdAndEstado(empresaId, EstadoInvitacion.PENDIENTE).stream()
				.map(InvitacionRepositoryAdapter::aDominio).toList();
	}

	@Override
	public Optional<Invitacion> pendientePorEmailYEmpresa(String email, UUID empresaId) {
		return jpa.findFirstByEmpresaIdAndEmailIgnoreCaseAndEstado(empresaId, email, EstadoInvitacion.PENDIENTE)
				.map(InvitacionRepositoryAdapter::aDominio);
	}

	private static Invitacion aDominio(InvitacionJpaEntity e) {
		return Invitacion.rehidratar(e.getId(), e.getEmpresaId(), e.getEmail(), e.getRol(), e.getSucursalIds(),
				e.getTokenHash(), e.getExpiraEn(), e.getEstado(), e.getAceptoTerminosEn());
	}
}
