package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Baja/reactivación de empleados (RF-8), acotada al tenant. Solo aplica a empleados de la empresa:
 * no se puede dar de baja al DUEÑO, ni a un CLIENTE del marketplace, ni a uno mismo.
 */
@Service
class EstadoDeEmpleadoService implements GestionarEstadoDeEmpleado {

	private final UsuarioRepository usuarios;

	EstadoDeEmpleadoService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional
	public Usuario desactivar(UUID empresaId, UUID actorId, UUID usuarioId) {
		if (usuarioId.equals(actorId)) {
			throw new IllegalArgumentException("No puedes darte de baja a ti mismo");
		}
		Usuario empleado = empleadoDelTenant(empresaId, usuarioId);
		return usuarios.guardar(empleado.desactivar());
	}

	@Override
	@Transactional
	public Usuario activar(UUID empresaId, UUID usuarioId) {
		return usuarios.guardar(empleadoDelTenant(empresaId, usuarioId).activar());
	}

	/** Carga el usuario, exige que sea de esta empresa y que sea un EMPLEADO (no dueño ni cliente). */
	private Usuario empleadoDelTenant(UUID empresaId, UUID usuarioId) {
		Usuario usuario = usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.orElseThrow(() -> new EmpleadoNoEncontrado(usuarioId));
		if (usuario.rol() == Rol.DUENO || usuario.rol() == Rol.SUPERADMIN || usuario.rol().esCliente()) {
			throw new IllegalArgumentException("Solo se puede dar de baja o reactivar a un empleado de la empresa");
		}
		return usuario;
	}
}
