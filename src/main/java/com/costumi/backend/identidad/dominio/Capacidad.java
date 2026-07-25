package com.costumi.backend.identidad.dominio;

/**
 * Catálogo exhaustivo de capacidades configurables de una tienda (Fase B, paso 5). Cada valor es un
 * <b>toggle</b> que se concede o niega por persona; el rol solo define el <b>preset</b> de defaults (ver
 * {@link PlantillaDeRol}). Cada capacidad pertenece a una {@link Seccion} (para agrupar en la pantalla) y
 * lleva el texto de <b>qué habilita</b> (decisión #8: claro, sin ambigüedad). Fuente: PLAN_PERMISOS_CATALOGO.md.
 *
 * <p>El nombre del enum es la clave estable que se persiste y se autoriza; no reordenar por semántica.
 */
public enum Capacidad {

	// --- INVENTARIO ---
	INVENTARIO_VER(Seccion.INVENTARIO, "Ver prendas, stock y avisos de stock bajo"),
	INVENTARIO_PRENDA_GESTIONAR(Seccion.INVENTARIO, "Crear y editar prendas y su foto"),
	INVENTARIO_PRENDA_ARCHIVAR(Seccion.INVENTARIO, "Archivar o reactivar una prenda"),
	INVENTARIO_STOCK_ENTRADA(Seccion.INVENTARIO, "Registrar entrada de mercadería"),
	INVENTARIO_STOCK_AJUSTAR(Seccion.INVENTARIO, "Corregir el conteo de stock (puede tapar faltantes)"),
	INVENTARIO_STOCK_MOVER(Seccion.INVENTARIO, "Mover stock (reorganizar)"),
	INVENTARIO_STOCK_TRANSFERIR(Seccion.INVENTARIO, "Transferir stock entre sucursales"),
	INVENTARIO_GRUPO_ELIMINAR(Seccion.INVENTARIO, "Eliminar una combinación de stock"),

	// --- CATALOGO ---
	CATALOGO_VER(Seccion.CATALOGO, "Ver categorías (de prendas y de disfraces) y etiquetas"),
	CATALOGO_CATEGORIAS_GESTIONAR(Seccion.CATALOGO, "Crear, editar y archivar categorías"),
	CATALOGO_ETIQUETAS_GESTIONAR(Seccion.CATALOGO, "Definir tipos de etiqueta y sus valores (variantes)"),

	// --- DISFRACES ---
	DISFRACES_VER(Seccion.DISFRACES, "Ver disfraces y su disponibilidad"),
	DISFRACES_GESTIONAR(Seccion.DISFRACES, "Crear y editar disfraces y su foto"),
	DISFRACES_ARCHIVAR(Seccion.DISFRACES, "Archivar o reactivar un disfraz"),

	// --- VENTAS ---
	VENTAS_VER(Seccion.VENTAS, "Ver ventas y totales"),
	VENTAS_REGISTRAR(Seccion.VENTAS, "Registrar una venta"),
	VENTAS_DESCUENTO(Seccion.VENTAS, "Aplicar descuento o precio especial en una venta (plata que se resigna)"),
	VENTAS_DEVOLVER(Seccion.VENTAS, "Devolver una venta (reembolso de stock y plata)"),

	// --- RENTAS ---
	RENTAS_VER(Seccion.RENTAS, "Ver rentas y su resumen"),
	RENTAS_REGISTRAR(Seccion.RENTAS, "Crear una renta"),
	RENTAS_ENTREGAR(Seccion.RENTAS, "Marcar la entrega de una renta"),
	RENTAS_DEVOLVER(Seccion.RENTAS, "Registrar la devolución (calcula la multa por retraso)"),
	RENTAS_CERRAR(Seccion.RENTAS, "Cerrar la renta y decidir el depósito (devolver o retener)"),
	RENTAS_CANCELAR(Seccion.RENTAS, "Cancelar una renta"),
	RENTAS_EXTENDER(Seccion.RENTAS, "Extender el plazo de una renta (afecta disponibilidad y cobro)"),

	// --- DEVOLUCIONES ---
	DEVOLUCIONES_VER(Seccion.DEVOLUCIONES, "Ver devoluciones"),
	DEVOLUCIONES_REGISTRAR(Seccion.DEVOLUCIONES, "Registrar una devolución"),

	// --- PAGOS ---
	PAGOS_VER(Seccion.PAGOS, "Ver pagos, saldos y comprobantes"),
	PAGOS_REGISTRAR(Seccion.PAGOS, "Registrar un pago (incluye pago mixto)"),
	PAGOS_COBRAR_EN_LINEA(Seccion.PAGOS, "Generar un cobro por pasarela de pago"),

	// --- CAJA ---
	CAJA_VER(Seccion.CAJA, "Ver turnos y movimientos de caja"),
	CAJA_ABRIR_TURNO(Seccion.CAJA, "Abrir un turno de caja"),
	CAJA_MOVIMIENTO(Seccion.CAJA, "Registrar un movimiento de caja"),
	CAJA_CERRAR_TURNO(Seccion.CAJA, "Cerrar un turno (arqueo y conciliación)"),

	// --- REEMBOLSOS ---
	REEMBOLSOS_VER(Seccion.REEMBOLSOS, "Ver reembolsos"),
	REEMBOLSOS_SOLICITAR(Seccion.REEMBOLSOS, "Solicitar o registrar un reembolso"),
	REEMBOLSOS_APROBAR(Seccion.REEMBOLSOS, "Aprobar un reembolso (plata que sale; alta confianza)"),
	REEMBOLSOS_RECHAZAR(Seccion.REEMBOLSOS, "Rechazar un reembolso"),

	// --- CLIENTES ---
	CLIENTES_VER(Seccion.CLIENTES, "Ver clientes, su historial y estado de cuenta (deudas)"),
	CLIENTES_CREAR(Seccion.CLIENTES, "Registrar un cliente"),
	CLIENTES_EDITAR(Seccion.CLIENTES, "Editar los datos de un cliente"),
	CLIENTES_ARCHIVAR(Seccion.CLIENTES, "Archivar o reactivar un cliente"),
	CLIENTES_LISTA_NEGRA(Seccion.CLIENTES, "Poner o quitar a un cliente de la lista negra (lo bloquea)"),

	// --- REPORTES ---
	REPORTES_VER(Seccion.REPORTES, "Ver reportes (ventas, rentas, estado de cuenta, rankings)"),

	// --- AUDITORIA ---
	AUDITORIA_VER(Seccion.AUDITORIA, "Ver quién hizo qué (trazabilidad sensible)"),

	// --- CONFIGURACION ---
	CONFIGURACION_VER(Seccion.CONFIGURACION, "Ver la configuración del local"),
	CONFIGURACION_EDITAR(Seccion.CONFIGURACION, "Cambiar los interruptores del local (cambia reglas de todo)"),
	CONFIGURACION_IMPORTAR_EXPORTAR(Seccion.CONFIGURACION, "Importar o exportar la configuración"),

	// --- SUCURSALES ---
	SUCURSALES_VER(Seccion.SUCURSALES, "Ver las sucursales de la empresa"),
	SUCURSALES_GESTIONAR(Seccion.SUCURSALES, "Crear, editar o archivar sucursales (incluye su foto)"),

	// --- IDENTIDAD_TIENDA ---
	IDENTIDAD_TIENDA_EDITAR(Seccion.IDENTIDAD_TIENDA, "Editar datos, logo/portada y horario de la tienda"),

	// --- NOTIFICACIONES ---
	NOTIFICACIONES_VER(Seccion.NOTIFICACIONES, "Ver el estado de los canales y las plantillas"),
	NOTIFICACIONES_PLANTILLAS_EDITAR(Seccion.NOTIFICACIONES, "Editar las plantillas de mensajes"),
	NOTIFICACIONES_ENVIAR(Seccion.NOTIFICACIONES, "Enviar una notificación manual"),
	NOTIFICACIONES_DISPARAR_AVISOS(Seccion.NOTIFICACIONES, "Disparar recordatorios y avisos (vencidas, próximas, stock bajo)"),
	NOTIFICACIONES_PROBAR_PUSH(Seccion.NOTIFICACIONES, "Probar el envío de push"),

	// --- EMPLEADOS ---
	EMPLEADOS_VER(Seccion.EMPLEADOS, "Ver el personal y su actividad"),
	EMPLEADOS_INVITAR(Seccion.EMPLEADOS, "Invitar gente a trabajar en la tienda"),
	EMPLEADOS_INVITACION_CANCELAR(Seccion.EMPLEADOS, "Cancelar una invitación pendiente"),
	EMPLEADOS_SUSPENDER(Seccion.EMPLEADOS, "Suspender o reactivar una membresía (reversible)"),
	EMPLEADOS_DAR_DE_BAJA(Seccion.EMPLEADOS, "Dar de baja a un empleado (despido definitivo)"),
	EMPLEADOS_CUENTA_ESTADO(Seccion.EMPLEADOS, "Desactivar o activar la cuenta (corta hasta el login)"),
	EMPLEADOS_CAMBIAR_ROL(Seccion.EMPLEADOS, "Cambiar el rol (preset) de otro empleado"),
	EMPLEADOS_EDITAR_PERMISOS(Seccion.EMPLEADOS, "Editar los permisos de otros (reparte confianza)"),
	EMPLEADOS_ASIGNAR_SUCURSALES(Seccion.EMPLEADOS, "Asignar o reasignar a qué sucursales opera cada uno");

	private final Seccion seccion;
	private final String descripcion;

	Capacidad(Seccion seccion, String descripcion) {
		this.seccion = seccion;
		this.descripcion = descripcion;
	}

	public Seccion seccion() {
		return seccion;
	}

	public String descripcion() {
		return descripcion;
	}
}
