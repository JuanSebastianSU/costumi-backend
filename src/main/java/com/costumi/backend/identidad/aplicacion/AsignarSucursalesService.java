package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Asignación de sucursales a un empleado (RF-1.2/8.1), acotada al tenant y a la pirámide de roles (B3):
 * solo se gestiona a empleados estrictamente por debajo del actor.
 */
@Service
class AsignarSucursalesService implements AsignarSucursales {

	private final AsignacionDeSucursalesRepository asignaciones;
	private final UsuarioRepository usuarios;
	private final AutoridadSobreSucursales autoridadSobreSucursales;

	AsignarSucursalesService(AsignacionDeSucursalesRepository asignaciones, UsuarioRepository usuarios,
			AutoridadSobreSucursales autoridadSobreSucursales) {
		this.asignaciones = asignaciones;
		this.usuarios = usuarios;
		this.autoridadSobreSucursales = autoridadSobreSucursales;
	}

	@Override
	@Transactional
	public void asignar(UUID empresaId, Rol actorRol, UUID actorId, UUID usuarioId, Set<UUID> sucursalIds) {
		exigirEmpleadoGestionable(empresaId, actorRol, usuarioId);
		autoridadSobreSucursales.exigirAsignables(empresaId, actorRol, actorId, sucursalIds);
		asignaciones.reemplazar(empresaId, usuarioId, sucursalIds);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> sucursalesDe(UUID empresaId, Rol actorRol, UUID usuarioId) {
		exigirEmpleadoGestionable(empresaId, actorRol, usuarioId);
		return asignaciones.sucursalesDe(usuarioId);
	}

	private void exigirEmpleadoGestionable(UUID empresaId, Rol actorRol, UUID usuarioId) {
		Usuario empleado = usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.orElseThrow(() -> new IllegalArgumentException("El empleado no existe en esta empresa"));
		if (!actorRol.puedeGestionarA(empleado.rol())) {
			throw new GestionDeEmpleadoNoPermitida();
		}
	}
}
