package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.ConsultaDeJerarquiaDeRoles;
import com.costumi.backend.identidad.dominio.Rol;
import org.springframework.stereotype.Service;

/** Implementa la consulta de jerarquía (B3) delegando en {@link Rol#puedeGestionarA(Rol)}. */
@Service
class ConsultaDeJerarquiaDeRolesService implements ConsultaDeJerarquiaDeRoles {

	@Override
	public boolean superaEstrictamente(String rolActor, String rolObjetivo) {
		Rol actor = parsear(rolActor);
		Rol objetivo = parsear(rolObjetivo);
		return actor != null && objetivo != null && actor.puedeGestionarA(objetivo);
	}

	private static Rol parsear(String rol) {
		if (rol == null) {
			return null;
		}
		try {
			return Rol.valueOf(rol);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
