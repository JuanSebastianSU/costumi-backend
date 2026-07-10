package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cambia el rol de un empleado (RF-8, G2), acotado al tenant y a la pirámide de roles (B3). Guardas:
 * el nuevo rol debe ser de empresa (no SuperAdmin/Cliente) y estrictamente por debajo del actor, y el actor
 * debe tener autoridad sobre el rol actual del empleado. Así no se asciende a un igual/superior ni se crea un DUEÑO.
 */
@Service
class CambiarRolDeEmpleadoService implements CambiarRolDeEmpleado {

	private final UsuarioRepository usuarios;

	CambiarRolDeEmpleadoService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional
	public Usuario ejecutar(UUID empresaId, Rol actorRol, UUID usuarioId, Rol nuevoRol) {
		if (nuevoRol == Rol.SUPERADMIN || nuevoRol == Rol.CLIENTE) {
			throw new IllegalArgumentException("El rol de un empleado debe ser de la empresa (no SuperAdmin ni Cliente)");
		}
		Usuario empleado = usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.orElseThrow(() -> new EmpleadoNoEncontrado(usuarioId));
		// B3: autoridad sobre el empleado actual Y sobre el rol destino (nadie asciende a un igual/superior).
		if (!actorRol.puedeGestionarA(empleado.rol()) || !actorRol.puedeGestionarA(nuevoRol)) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		return usuarios.guardar(empleado.cambiarRol(nuevoRol));
	}
}
