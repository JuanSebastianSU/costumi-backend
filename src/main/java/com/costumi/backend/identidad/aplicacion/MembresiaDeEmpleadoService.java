package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementa la desvinculación de dos vías (Fase B, paso 3). Opera SOLO sobre el estado de la <b>membresía</b>
 * (no toca el {@code Usuario} base). El corte de acceso lo aplica {@link ContextoDeTrabajo} al emitir la
 * sesión: si el rol es de trabajo pero la membresía no está activa, la sesión cae a cliente. Todo respeta la
 * pirámide (RF-1.3/B3).
 */
@Service
class MembresiaDeEmpleadoService implements GestionarMembresiaDeEmpleado {

	private final MembresiaRepository membresias;

	MembresiaDeEmpleadoService(MembresiaRepository membresias) {
		this.membresias = membresias;
	}

	@Override
	@Transactional
	public Membresia suspender(UUID empresaId, Rol actorRol, UUID usuarioId) {
		return membresias.guardar(gestionable(empresaId, actorRol, usuarioId).suspender());
	}

	@Override
	@Transactional
	public Membresia reactivar(UUID empresaId, Rol actorRol, UUID usuarioId) {
		return membresias.guardar(gestionable(empresaId, actorRol, usuarioId).reactivar());
	}

	@Override
	@Transactional
	public Membresia quitar(UUID empresaId, Rol actorRol, UUID usuarioId) {
		return membresias.guardar(gestionable(empresaId, actorRol, usuarioId).darDeBajaPorDueno());
	}

	@Override
	@Transactional
	public Membresia desvincularme(UUID usuarioId) {
		Membresia m = membresias.activaDeUsuario(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("No tenés una membresía de trabajo activa"));
		if (m.rol() == Rol.DUENO) {
			throw new IllegalArgumentException("Sos el dueño de la tienda; no podés desvincularte de tu propia empresa");
		}
		return membresias.guardar(m.darDeBajaPorEmpleado());
	}

	/** Carga la membresía del empleado en el tenant y exige autoridad piramidal del actor sobre su rol. */
	private Membresia gestionable(UUID empresaId, Rol actorRol, UUID usuarioId) {
		Membresia m = membresias.buscar(usuarioId, empresaId)
				.orElseThrow(() -> new EmpleadoNoEncontrado(usuarioId));
		if (!actorRol.puedeGestionarA(m.rol())) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		return m;
	}
}
