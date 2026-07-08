package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Asignación de sucursales a un empleado (RF-1.2/8.1), acotada al tenant. */
@Service
class AsignarSucursalesService implements AsignarSucursales {

	private final AsignacionDeSucursalesRepository asignaciones;
	private final UsuarioRepository usuarios;
	private final SucursalRepository sucursales;

	AsignarSucursalesService(AsignacionDeSucursalesRepository asignaciones, UsuarioRepository usuarios,
			SucursalRepository sucursales) {
		this.asignaciones = asignaciones;
		this.usuarios = usuarios;
		this.sucursales = sucursales;
	}

	@Override
	@Transactional
	public void asignar(UUID empresaId, UUID usuarioId, Set<UUID> sucursalIds) {
		exigirEmpleadoDelTenant(empresaId, usuarioId);
		for (UUID sucursalId : sucursalIds) {
			boolean delTenant = sucursales.buscarPorId(sucursalId)
					.filter(s -> s.empresaId().equals(empresaId))
					.isPresent();
			if (!delTenant) {
				throw new IllegalArgumentException("La sucursal no existe en esta empresa");
			}
		}
		asignaciones.reemplazar(empresaId, usuarioId, sucursalIds);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> sucursalesDe(UUID empresaId, UUID usuarioId) {
		exigirEmpleadoDelTenant(empresaId, usuarioId);
		return asignaciones.sucursalesDe(usuarioId);
	}

	private void exigirEmpleadoDelTenant(UUID empresaId, UUID usuarioId) {
		boolean delTenant = usuarios.buscarPorId(usuarioId)
				.filter(u -> empresaId.equals(u.empresaId()))
				.isPresent();
		if (!delTenant) {
			throw new IllegalArgumentException("El empleado no existe en esta empresa");
		}
	}
}
