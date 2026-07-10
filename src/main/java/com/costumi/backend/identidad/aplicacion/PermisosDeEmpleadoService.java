package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Permiso;
import com.costumi.backend.identidad.dominio.PermisoDeEmpleadoRepository;
import com.costumi.backend.identidad.dominio.PlantillaDeRol;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Seccion;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Permisos granulares por empleado (RF-1.5): editor (matriz plantilla ± overrides) y chequeo. */
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
	public boolean bloqueado(UUID usuarioId, Seccion seccion, AccionDePermiso accion) {
		return overrides.valor(usuarioId, seccion, accion).map(concedido -> !concedido).orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PermisoEfectivo> matriz(UUID empresaId, Rol actorRol, UUID usuarioId) {
		Rol rol = empleadoGestionable(empresaId, actorRol, usuarioId).rol();
		Set<Permiso> plantilla = PlantillaDeRol.permisosDe(rol);
		List<PermisoEfectivo> matriz = new ArrayList<>();
		for (Seccion seccion : Seccion.values()) {
			for (AccionDePermiso accion : AccionDePermiso.values()) {
				Optional<Boolean> override = overrides.valor(usuarioId, seccion, accion);
				boolean concedido = override.orElseGet(() -> plantilla.contains(new Permiso(seccion, accion)));
				matriz.add(new PermisoEfectivo(seccion, accion, concedido));
			}
		}
		return matriz;
	}

	@Override
	@Transactional
	public void establecer(UUID empresaId, Rol actorRol, UUID usuarioId, Seccion seccion, AccionDePermiso accion,
			boolean concedido) {
		empleadoGestionable(empresaId, actorRol, usuarioId); // §5.4 + pirámide (B3)
		overrides.establecer(empresaId, usuarioId, seccion, accion, concedido);
	}

	/**
	 * Carga el empleado del tenant y exige que el actor tenga autoridad jerárquica sobre él (RF-1.3, B3):
	 * así un empleado no edita sus propios permisos ni los de un igual/superior (p. ej. re-concederse lo
	 * que el dueño le quitó).
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
