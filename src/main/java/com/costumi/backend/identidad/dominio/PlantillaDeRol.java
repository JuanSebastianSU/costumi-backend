package com.costumi.backend.identidad.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Preset de capacidades por rol (Fase B, paso 5): lo que cada rol tiene <b>por defecto</b>. Es el punto de
 * partida editable por persona (matriz efectiva = preset ± overrides). El rol NO es la autoridad: es solo el
 * preset. DUEÑO/ENCARGADO parten con todo; los operativos con lo suyo. Fuente: PLAN_PERMISOS_CATALOGO.md.
 */
public final class PlantillaDeRol {

	private PlantillaDeRol() {
	}

	/** Capacidades por defecto del rol (antes de los overrides por empleado). */
	public static Set<Capacidad> capacidadesDe(Rol rol) {
		return switch (rol) {
			case SUPERADMIN, DUENO, ENCARGADO -> EnumSet.allOf(Capacidad.class);
			case MOSTRADOR -> EnumSet.copyOf(OPERATIVO_MOSTRADOR);
			case ATENCION -> conNotificacionEnviar(OPERATIVO_MOSTRADOR);
			case BODEGA -> EnumSet.copyOf(OPERATIVO_BODEGA);
			case CLIENTE -> EnumSet.noneOf(Capacidad.class);
		};
	}

	private static final Set<Capacidad> OPERATIVO_MOSTRADOR = EnumSet.of(
			Capacidad.INVENTARIO_VER,
			Capacidad.DISFRACES_VER,
			Capacidad.VENTAS_VER, Capacidad.VENTAS_REGISTRAR, Capacidad.VENTAS_DEVOLVER,
			Capacidad.RENTAS_VER, Capacidad.RENTAS_REGISTRAR, Capacidad.RENTAS_ENTREGAR,
			Capacidad.RENTAS_DEVOLVER, Capacidad.RENTAS_CERRAR,
			Capacidad.DEVOLUCIONES_VER, Capacidad.DEVOLUCIONES_REGISTRAR,
			Capacidad.PAGOS_VER, Capacidad.PAGOS_REGISTRAR, Capacidad.PAGOS_COBRAR_EN_LINEA,
			Capacidad.CAJA_VER, Capacidad.CAJA_ABRIR_TURNO, Capacidad.CAJA_MOVIMIENTO, Capacidad.CAJA_CERRAR_TURNO,
			Capacidad.REEMBOLSOS_VER, Capacidad.REEMBOLSOS_SOLICITAR,
			Capacidad.CLIENTES_VER, Capacidad.CLIENTES_CREAR, Capacidad.CLIENTES_EDITAR);

	private static final Set<Capacidad> OPERATIVO_BODEGA = EnumSet.of(
			Capacidad.INVENTARIO_VER, Capacidad.INVENTARIO_PRENDA_GESTIONAR, Capacidad.INVENTARIO_PRENDA_ARCHIVAR,
			Capacidad.INVENTARIO_STOCK_ENTRADA, Capacidad.INVENTARIO_STOCK_AJUSTAR, Capacidad.INVENTARIO_STOCK_MOVER,
			Capacidad.INVENTARIO_STOCK_TRANSFERIR,
			Capacidad.CATALOGO_VER,
			Capacidad.DISFRACES_VER);

	private static Set<Capacidad> conNotificacionEnviar(Set<Capacidad> base) {
		Set<Capacidad> set = EnumSet.copyOf(base);
		set.add(Capacidad.NOTIFICACIONES_ENVIAR);
		return set;
	}
}
