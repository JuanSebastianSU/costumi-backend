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

	/**
	 * Nivel jerárquico dentro de la empresa (mayor = más autoridad, RF-1.3): Dueño(3) &gt; Encargado(2) &gt;
	 * operativos Mostrador/Bodega/Atención(1). Plataforma y cliente quedan fuera de la pirámide (0).
	 */
	public int nivelJerarquico() {
		return switch (this) {
			case DUENO -> 3;
			case ENCARGADO -> 2;
			case MOSTRADOR, BODEGA, ATENCION -> 1;
			case SUPERADMIN, CLIENTE -> 0;
		};
	}

	/**
	 * ¿Un actor con este rol puede <b>gestionar</b> (alta/baja, permisos, sucursales) o <b>crear</b> a un
	 * empleado de rol {@code otro}? Solo lo estrictamente por debajo en la pirámide (RF-1.3): nunca un
	 * igual, un superior, ni a sí mismo. (No habilita por sí solo crear roles fuera de la empresa como
	 * SuperAdmin/Cliente: eso se rechaza aparte.)
	 */
	public boolean puedeGestionarA(Rol otro) {
		return this.nivelJerarquico() > otro.nivelJerarquico();
	}
}
