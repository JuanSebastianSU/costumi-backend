package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Capacidad;
import com.costumi.backend.identidad.dominio.PermisoDeEmpleadoRepository;
import com.costumi.backend.identidad.dominio.PlantillaDeRol;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Matriz de capacidades por empleado (Fase B, paso 5): editor (preset ± overrides) y chequeo de bloqueo. */
@Service
class PermisosDeEmpleadoService implements GestionarPermisosDeEmpleado, ConsultaDePermisos {

	private final PermisoDeEmpleadoRepository overrides;
	private final UsuarioRepository usuarios;

	PermisosDeEmpleadoService(PermisoDeEmpleadoRepository overrides, UsuarioRepository usuarios) {
		this.overrides = overrides;
		this.usuarios = usuarios;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean bloqueado(UUID usuarioId, Capacidad capacidad) {
		return overrides.valor(usuarioId, capacidad).map(concedido -> !concedido).orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CapacidadEfectiva> matriz(UUID empresaId, Rol actorRol, UUID usuarioId) {
		Rol rol = empleadoGestionable(empresaId, actorRol, usuarioId).rol();
		List<CapacidadEfectiva> matriz = new ArrayList<>();
		for (Capacidad capacidad : Capacidad.values()) {
			matriz.add(new CapacidadEfectiva(capacidad, efectiva(usuarioId, rol, capacidad)));
		}
		return matriz;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Capacidad> mias(UUID usuarioId, Rol rol) {
		List<Capacidad> concedidas = new ArrayList<>();
		for (Capacidad capacidad : Capacidad.values()) {
			if (efectiva(usuarioId, rol, capacidad)) {
				concedidas.add(capacidad);
			}
		}
		return concedidas;
	}

	@Override
	@Transactional
	public void establecer(UUID empresaId, Rol actorRol, UUID actorId, UUID usuarioId, Capacidad capacidad,
			boolean concedido) {
		empleadoGestionable(empresaId, actorRol, usuarioId); // pirámide (B3): solo hacia abajo
		// "No podés conceder lo que no tenés": al otorgar, el actor debe tener la capacidad (efectiva).
		if (concedido && !efectiva(actorId, actorRol, capacidad)) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		overrides.establecer(empresaId, usuarioId, capacidad, concedido);
	}

	/** Capacidad efectiva = override si existe, si no el preset del rol. */
	private boolean efectiva(UUID usuarioId, Rol rol, Capacidad capacidad) {
		Set<Capacidad> preset = PlantillaDeRol.capacidadesDe(rol);
		return overrides.valor(usuarioId, capacidad).orElseGet(() -> preset.contains(capacidad));
	}

	/**
	 * Carga el empleado del tenant y exige que el actor tenga autoridad jerárquica sobre él (RF-1.3, B3):
	 * así un empleado no edita sus propios permisos ni los de un igual/superior.
	 */
	private Usuario empleadoGestionable(UUID empresaId, Rol actorRol, UUID usuarioId) {
		Usuario empleado = usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.orElseThrow(() -> new IllegalArgumentException("El empleado no existe en esta empresa"));
		if (!actorRol.puedeGestionarA(empleado.rol())) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		return empleado;
	}
}
