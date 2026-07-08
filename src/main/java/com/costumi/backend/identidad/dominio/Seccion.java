package com.costumi.backend.identidad.dominio;

/**
 * Sección/operación sobre la que se conceden permisos granulares por empleado (RF-1.5). Cada request
 * operativo se mapea a una sección + una {@link AccionDePermiso} (ver / actuar).
 */
public enum Seccion {
	INVENTARIO,
	DISFRACES,
	VENTAS,
	RENTAS,
	DEVOLUCIONES,
	PAGOS,
	CAJA,
	REPORTES,
	CLIENTES,
	CONFIGURACION,
	NOTIFICACIONES,
	EMPLEADOS
}
