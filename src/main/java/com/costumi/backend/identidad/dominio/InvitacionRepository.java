package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: invitaciones de trabajo (Fase B). No se filtra por tenant (la aceptación es pública). */
public interface InvitacionRepository {

	Invitacion guardar(Invitacion invitacion);

	/** Busca por el hash del token (para aceptar/ver la invitación desde el enlace). */
	Optional<Invitacion> buscarPorHash(String tokenHash);

	Optional<Invitacion> buscarPorId(UUID id);

	/** Invitaciones PENDIENTES de una empresa (para listarlas / evitar duplicados a un mismo email). */
	List<Invitacion> pendientesDeEmpresa(UUID empresaId);

	/** Invitación PENDIENTE para ese email en esa empresa, si existe (una a la vez por email/empresa). */
	Optional<Invitacion> pendientePorEmailYEmpresa(String email, UUID empresaId);
}
