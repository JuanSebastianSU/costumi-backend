package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.ConsultaDeAsignaciones;
import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementa {@link ConsultaDeAsignaciones}: un empleado de rol <b>acotado</b> (Mostrador/Bodega/Atención)
 * solo opera en las sucursales que tiene asignadas (RF-1.2, B2). Los roles de gestión (Dueño/Encargado) y
 * los actores que no son empleados del tenant (CLIENTE del marketplace, id nulo) no están acotados.
 */
@Service
class ConsultaDeAsignacionesService implements ConsultaDeAsignaciones {

	private final UsuarioRepository usuarios;
	private final AsignacionDeSucursalesRepository asignaciones;

	ConsultaDeAsignacionesService(UsuarioRepository usuarios, AsignacionDeSucursalesRepository asignaciones) {
		this.usuarios = usuarios;
		this.asignaciones = asignaciones;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean empleadoPuedeOperarEn(UUID empresaId, UUID empleadoId, UUID sucursalId) {
		if (empleadoId == null) {
			return true;
		}
		Usuario empleado = usuarios.buscarPorId(empleadoId)
				.filter(usuario -> empresaId.equals(usuario.empresaId()))
				.orElse(null);
		if (empleado == null || !esAcotadoPorSucursal(empleado.rol())) {
			return true;
		}
		return asignaciones.sucursalesDe(empleadoId).contains(sucursalId);
	}

	/** Roles operativos "de piso": solo operan donde están asignados. Gestión (Dueño/Encargado) no. */
	private static boolean esAcotadoPorSucursal(Rol rol) {
		return rol == Rol.MOSTRADOR || rol == Rol.BODEGA || rol == Rol.ATENCION;
	}
}
