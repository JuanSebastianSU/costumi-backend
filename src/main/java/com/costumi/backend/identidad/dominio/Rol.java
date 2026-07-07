package com.costumi.backend.identidad.dominio;

/**
 * Roles de acceso. Plantillas de RF-1.3 (Mostrador/Bodega/Atención/Encargado/Dueño)
 * más el SuperAdmin de plataforma (RF-15.3), único rol que cruza tenants, y el CLIENTE
 * (usuario final del marketplace: se auto-registra, navega todas las tiendas y no pertenece
 * a ninguna empresa hasta que —si abre su local— es promovido a Dueño).
 */
public enum Rol {
	SUPERADMIN,
	DUENO,
	ENCARGADO,
	MOSTRADOR,
	BODEGA,
	ATENCION,
	CLIENTE;

	/** El SuperAdmin es de plataforma (no pertenece a una empresa). */
	public boolean esDePlataforma() {
		return this == SUPERADMIN;
	}

	/** El cliente es un usuario final del marketplace; no pertenece a ninguna empresa. */
	public boolean esCliente() {
		return this == CLIENTE;
	}

	/**
	 * Rol operativo de una empresa (personal): Dueño/Encargado/Mostrador/Bodega/Atención.
	 * Solo estos roles deben pertenecer a un tenant (tener {@code empresa_id}).
	 */
	public boolean requiereEmpresa() {
		return !esDePlataforma() && !esCliente();
	}
}
