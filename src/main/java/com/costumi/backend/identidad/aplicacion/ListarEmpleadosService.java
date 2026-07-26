package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Lista el personal del tenant (G1). Aplica la pirámide de roles (B3): el actor solo ve a quienes puede
 * gestionar (estrictamente por debajo suyo), y adjunta las sucursales asignadas de cada uno.
 */
@Service
class ListarEmpleadosService implements ListarEmpleados {

	private final UsuarioRepository usuarios;
	private final AsignacionDeSucursalesRepository asignaciones;
	private final MembresiaRepository membresias;

	ListarEmpleadosService(UsuarioRepository usuarios, AsignacionDeSucursalesRepository asignaciones,
			MembresiaRepository membresias) {
		this.usuarios = usuarios;
		this.asignaciones = asignaciones;
		this.membresias = membresias;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmpleadoDelTenant> delTenant(UUID empresaId, Rol actorRol) {
		return usuarios.listarPorEmpresa(empresaId).stream()
				.filter(usuario -> actorRol.puedeGestionarA(usuario.rol()))
				.filter(usuario -> !dadoDeBaja(usuario.id(), empresaId))
				.sorted(Comparator.comparing(usuario -> usuario.rol().nivelJerarquico(), Comparator.reverseOrder()))
				.map(usuario -> new EmpleadoDelTenant(usuario.id(), usuario.email(), usuario.rol(), usuario.activo(),
						usuario.fotoUrl(), asignaciones.sucursalesDe(usuario.id())))
				.toList();
	}

	/** Un empleado con membresía dada de baja (despedido o que se fue) ya no es parte del personal. */
	private boolean dadoDeBaja(UUID usuarioId, UUID empresaId) {
		return membresias.buscar(usuarioId, empresaId).map(Membresia::esBaja).orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public com.costumi.backend.compartido.Pagina<EmpleadoDelTenant> delTenant(UUID empresaId, Rol actorRol, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina) {
		String filtro = buscar == null || buscar.isBlank() ? null : buscar.trim().toLowerCase();
		List<EmpleadoDelTenant> visibles = delTenant(empresaId, actorRol).stream()
				.filter(e -> filtro == null || e.email().toLowerCase().contains(filtro))
				.toList();
		int desde = Math.min(pagina.pagina() * pagina.tamano(), visibles.size());
		int hasta = Math.min(desde + pagina.tamano(), visibles.size());
		return com.costumi.backend.compartido.Pagina.de(visibles.subList(desde, hasta), visibles.size(), pagina);
	}
}
