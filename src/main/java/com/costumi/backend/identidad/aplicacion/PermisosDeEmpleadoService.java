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
	public List<PermisoEfectivo> matriz(UUID empresaId, UUID usuarioId) {
		Rol rol = empleadoDelTenant(empresaId, usuarioId).rol();
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
	public void establecer(UUID empresaId, UUID usuarioId, Seccion seccion, AccionDePermiso accion,
			boolean concedido) {
		empleadoDelTenant(empresaId, usuarioId); // §5.4: solo empleados de la empresa
		overrides.establecer(empresaId, usuarioId, seccion, accion, concedido);
	}

	private Usuario empleadoDelTenant(UUID empresaId, UUID usuarioId) {
		return usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.orElseThrow(() -> new IllegalArgumentException("El empleado no existe en esta empresa"));
	}
}
