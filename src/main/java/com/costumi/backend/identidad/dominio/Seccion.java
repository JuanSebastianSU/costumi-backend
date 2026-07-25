package com.costumi.backend.identidad.dominio;

/**
 * Sección de la app usada para <b>agrupar</b> las capacidades configurables en la pantalla de permisos
 * (Fase B, paso 5). Cada {@link Capacidad} pertenece a una sección. Reemplaza el enfoque anterior de
 * "sección × (VER/ACCION)" por un catálogo de capacidades finas (ver {@code PLAN_PERMISOS_CATALOGO.md}).
 */
public enum Seccion {
	INVENTARIO,
	CATALOGO,
	DISFRACES,
	VENTAS,
	RENTAS,
	DEVOLUCIONES,
	PAGOS,
	CAJA,
	REEMBOLSOS,
	CLIENTES,
	REPORTES,
	AUDITORIA,
	CONFIGURACION,
	SUCURSALES,
	IDENTIDAD_TIENDA,
	NOTIFICACIONES,
	EMPLEADOS
}
