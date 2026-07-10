package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Baja/reactivación de empleados (RF-8), acotada al tenant y a la <b>pirámide de roles</b> (RF-1.3, B3):
 * solo se puede dar de baja/reactivar a un empleado estrictamente por debajo del actor (nunca a un igual,
 * a un superior, ni a uno mismo). El Dueño/SuperAdmin no son objetivos válidos (no están por debajo).
 */
@Service
class EstadoDeEmpleadoService implements GestionarEstadoDeEmpleado {

	private final UsuarioRepository usuarios;

	EstadoDeEmpleadoService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional
	public Usuario desactivar(UUID empresaId, Rol actorRol, UUID actorId, UUID usuarioId) {
		Usuario empleado = empleadoGestionable(empresaId, actorRol, usuarioId);
		return usuarios.guardar(empleado.desactivar());
	}

	@Override
	@Transactional
	public Usuario activar(UUID empresaId, Rol actorRol, UUID usuarioId) {
		return usuarios.guardar(empleadoGestionable(empresaId, actorRol, usuarioId).activar());
	}

	/**
	 * Carga el empleado, exige que sea del tenant y que el actor tenga autoridad jerárquica sobre él. La
	 * regla de pirámide cubre además el auto-servicio (mismo rol → no permitido) y al Dueño/SuperAdmin.
	 */
	private Usuario empleadoGestionable(UUID empresaId, Rol actorRol, UUID usuarioId) {
		Usuario usuario = usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.orElseThrow(() -> new EmpleadoNoEncontrado(usuarioId));
		if (!actorRol.puedeGestionarA(usuario.rol())) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		return usuario;
	}
}
