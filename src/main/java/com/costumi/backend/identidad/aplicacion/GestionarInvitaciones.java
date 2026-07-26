package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Invitaciones de trabajo (Fase B, paso 3): invitar por email (alta = invitación, no se crea cuenta suelta),
 * ver la invitación desde el enlace, aceptarla (con T&C, creando la cuenta si hace falta), listarlas y
 * cancelarlas. Todo respeta la pirámide y la regla de una-membresía-activa.
 */
public interface GestionarInvitaciones {

	record InvitarComando(UUID empresaId, Rol actorRol, UUID actorId, String email, Rol rol, Set<UUID> sucursalIds) {
	}

	/** Resultado de invitar: incluye el token/enlace de aceptación (para compartir aunque no haya email real). */
	record InvitacionCreada(UUID id, String email, Rol rol, String token, String enlace) {
	}

	/** Vista pública de una invitación (desde el enlace), para que la app muestre a qué tienda/rol la invitan. */
	record InvitacionVista(String empresaNombre, Rol rol, String email, boolean necesitaCuenta) {
	}

	/** Una invitación pendiente listada para la tienda. */
	record InvitacionPendiente(UUID id, String email, Rol rol, Instant expiraEn) {
	}

	record AceptarComando(String token, String password, boolean aceptaTerminos) {
	}

	InvitacionCreada invitar(InvitarComando comando);

	/**
	 * Reenvía una invitación pendiente: genera un enlace/token NUEVO con el mismo email, rol y sucursales,
	 * cancela el anterior y vuelve a enviar el email. Útil si el correo no llegó (SMTP) o venció el enlace.
	 */
	InvitacionCreada reenviar(UUID empresaId, Rol actorRol, UUID actorId, UUID invitacionId);

	InvitacionVista ver(String token);

	/** Acepta la invitación y devuelve una sesión (auto-login como la persona). */
	Credenciales aceptar(AceptarComando comando);

	List<InvitacionPendiente> pendientes(UUID empresaId);

	void cancelar(UUID empresaId, Rol actorRol, UUID invitacionId);
}
