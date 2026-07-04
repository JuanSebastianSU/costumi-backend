package com.costumi.backend.identidad.dominio;

/**
 * Roles de acceso. Plantillas de RF-1.3 (Mostrador/Bodega/Atención/Encargado/Dueño)
 * más el SuperAdmin de plataforma (RF-15.3), único rol que cruza tenants.
 */
public enum Rol {
	SUPERADMIN,
	DUENO,
	ENCARGADO,
	MOSTRADOR,
	BODEGA,
	ATENCION;

	/** El SuperAdmin es de plataforma (no pertenece a una empresa). */
	public boolean esDePlataforma() {
		return this == SUPERADMIN;
	}
}
