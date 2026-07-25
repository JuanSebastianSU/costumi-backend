package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Capacidad;

import java.util.Optional;

/**
 * Mapea un request (método + ruta) a la {@link Capacidad} fina que exige, para el chequeo de permisos
 * granulares (Fase B, paso 5). Las rutas no mapeadas no llevan chequeo granular (las cubre la autorización
 * por rol de {@code SecurityConfig}). Las rutas propias (que contienen {@code /me} o {@code /mios}) nunca se
 * bloquean: uno siempre ve/gestiona lo suyo.
 */
final class MapaDeSecciones {

	private MapaDeSecciones() {
	}

	static Optional<Capacidad> capacidadRequerida(String metodo, String ruta) {
		if (esPropia(ruta)) {
			return Optional.empty();
		}
		boolean ver = "GET".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo);
		return Optional.ofNullable(capacidadDe(metodo, ruta, ver));
	}

	private static Capacidad capacidadDe(String metodo, String ruta, boolean ver) {
		if (en(ruta, "/api/v1/empleados")) {
			return empleados(ruta, ver);
		}
		if (en(ruta, "/api/v1/prendas") || en(ruta, "/api/v1/grupos-stock")) {
			return ver ? Capacidad.INVENTARIO_VER : inventario(metodo, ruta);
		}
		if (en(ruta, "/api/v1/categorias") || en(ruta, "/api/v1/tipos-etiqueta")) {
			return ver ? Capacidad.CATALOGO_VER
					: (en(ruta, "/api/v1/tipos-etiqueta") ? Capacidad.CATALOGO_ETIQUETAS_GESTIONAR
							: Capacidad.CATALOGO_CATEGORIAS_GESTIONAR);
		}
		if (en(ruta, "/api/v1/disfraces")) {
			return ver ? Capacidad.DISFRACES_VER
					: (termina(ruta, "/archivar") || termina(ruta, "/activar") ? Capacidad.DISFRACES_ARCHIVAR
							: Capacidad.DISFRACES_GESTIONAR);
		}
		if (en(ruta, "/api/v1/ventas")) {
			return ver ? Capacidad.VENTAS_VER
					: (termina(ruta, "/devolver") ? Capacidad.VENTAS_DEVOLVER : Capacidad.VENTAS_REGISTRAR);
		}
		if (en(ruta, "/api/v1/rentas")) {
			return ver ? Capacidad.RENTAS_VER : rentas(ruta);
		}
		if (en(ruta, "/api/v1/devoluciones")) {
			return ver ? Capacidad.DEVOLUCIONES_VER : Capacidad.DEVOLUCIONES_REGISTRAR;
		}
		if (en(ruta, "/api/v1/pagos")) {
			return ver ? Capacidad.PAGOS_VER
					: (termina(ruta, "/intento") ? Capacidad.PAGOS_COBRAR_EN_LINEA : Capacidad.PAGOS_REGISTRAR);
		}
		if (en(ruta, "/api/v1/reembolsos")) {
			return ver ? Capacidad.REEMBOLSOS_VER : reembolsos(ruta);
		}
		if (en(ruta, "/api/v1/caja")) {
			return ver ? Capacidad.CAJA_VER : caja(ruta);
		}
		if (en(ruta, "/api/v1/reportes")) {
			return Capacidad.REPORTES_VER;
		}
		if (en(ruta, "/api/v1/auditoria")) {
			return Capacidad.AUDITORIA_VER;
		}
		if (en(ruta, "/api/v1/clientes")) {
			return ver ? Capacidad.CLIENTES_VER : clientes(ruta);
		}
		if (en(ruta, "/api/v1/configuracion")) {
			return ver ? Capacidad.CONFIGURACION_VER
					: (termina(ruta, "/import") ? Capacidad.CONFIGURACION_IMPORTAR_EXPORTAR
							: Capacidad.CONFIGURACION_EDITAR);
		}
		if (en(ruta, "/api/v1/empresas") && ruta.contains("/sucursales")) {
			return ver ? Capacidad.SUCURSALES_VER : Capacidad.SUCURSALES_GESTIONAR;
		}
		if (en(ruta, "/api/v1/empresas") && ruta.contains("/mia")) {
			return ver ? null : Capacidad.IDENTIDAD_TIENDA_EDITAR; // ver la propia tienda no se bloquea
		}
		if (en(ruta, "/api/v1/notificaciones")) {
			return ver ? Capacidad.NOTIFICACIONES_VER : notificaciones(ruta);
		}
		return null;
	}

	private static Capacidad empleados(String ruta, boolean ver) {
		if (ver) {
			if (termina(ruta, "/sucursales")) {
				return Capacidad.EMPLEADOS_ASIGNAR_SUCURSALES;
			}
			if (termina(ruta, "/permisos")) {
				return Capacidad.EMPLEADOS_EDITAR_PERMISOS;
			}
			return Capacidad.EMPLEADOS_VER;
		}
		if (ruta.contains("/invitaciones")) {
			return Capacidad.EMPLEADOS_INVITACION_CANCELAR;
		}
		if (termina(ruta, "/suspender") || termina(ruta, "/reactivar")) {
			return Capacidad.EMPLEADOS_SUSPENDER;
		}
		if (termina(ruta, "/quitar")) {
			return Capacidad.EMPLEADOS_DAR_DE_BAJA;
		}
		if (termina(ruta, "/desactivar") || termina(ruta, "/activar")) {
			return Capacidad.EMPLEADOS_CUENTA_ESTADO;
		}
		if (termina(ruta, "/rol")) {
			return Capacidad.EMPLEADOS_CAMBIAR_ROL;
		}
		if (termina(ruta, "/permisos")) {
			return Capacidad.EMPLEADOS_EDITAR_PERMISOS;
		}
		if (termina(ruta, "/sucursales")) {
			return Capacidad.EMPLEADOS_ASIGNAR_SUCURSALES;
		}
		return Capacidad.EMPLEADOS_INVITAR; // POST /empleados (alta = invitar)
	}

	private static Capacidad inventario(String metodo, String ruta) {
		if (termina(ruta, "/entrada")) {
			return Capacidad.INVENTARIO_STOCK_ENTRADA;
		}
		if (termina(ruta, "/ajuste")) {
			return Capacidad.INVENTARIO_STOCK_AJUSTAR;
		}
		if (termina(ruta, "/mover")) {
			return Capacidad.INVENTARIO_STOCK_MOVER;
		}
		if (termina(ruta, "/transferir")) {
			return Capacidad.INVENTARIO_STOCK_TRANSFERIR;
		}
		if (termina(ruta, "/archivar") || termina(ruta, "/activar")) {
			return Capacidad.INVENTARIO_PRENDA_ARCHIVAR;
		}
		if ("DELETE".equalsIgnoreCase(metodo)) {
			return Capacidad.INVENTARIO_GRUPO_ELIMINAR;
		}
		return Capacidad.INVENTARIO_PRENDA_GESTIONAR;
	}

	private static Capacidad rentas(String ruta) {
		if (termina(ruta, "/entregar")) {
			return Capacidad.RENTAS_ENTREGAR;
		}
		if (termina(ruta, "/devolver")) {
			return Capacidad.RENTAS_DEVOLVER;
		}
		if (termina(ruta, "/cerrar")) {
			return Capacidad.RENTAS_CERRAR;
		}
		if (termina(ruta, "/cancelar")) {
			return Capacidad.RENTAS_CANCELAR;
		}
		if (termina(ruta, "/extender")) {
			return Capacidad.RENTAS_EXTENDER;
		}
		return Capacidad.RENTAS_REGISTRAR;
	}

	private static Capacidad reembolsos(String ruta) {
		if (termina(ruta, "/aprobar")) {
			return Capacidad.REEMBOLSOS_APROBAR;
		}
		if (termina(ruta, "/rechazar")) {
			return Capacidad.REEMBOLSOS_RECHAZAR;
		}
		return Capacidad.REEMBOLSOS_SOLICITAR;
	}

	private static Capacidad caja(String ruta) {
		if (termina(ruta, "/cerrar")) {
			return Capacidad.CAJA_CERRAR_TURNO;
		}
		if (termina(ruta, "/movimientos")) {
			return Capacidad.CAJA_MOVIMIENTO;
		}
		return Capacidad.CAJA_ABRIR_TURNO;
	}

	private static Capacidad clientes(String ruta) {
		if (termina(ruta, "/lista-negra")) {
			return Capacidad.CLIENTES_LISTA_NEGRA;
		}
		if (termina(ruta, "/archivar") || termina(ruta, "/activar")) {
			return Capacidad.CLIENTES_ARCHIVAR;
		}
		if (ruta.equals("/api/v1/clientes")) {
			return Capacidad.CLIENTES_CREAR;
		}
		return Capacidad.CLIENTES_EDITAR;
	}

	private static Capacidad notificaciones(String ruta) {
		if (ruta.contains("/plantillas")) {
			return Capacidad.NOTIFICACIONES_PLANTILLAS_EDITAR;
		}
		if (ruta.contains("/probar-push")) {
			return Capacidad.NOTIFICACIONES_PROBAR_PUSH;
		}
		if (ruta.contains("/recordar") || ruta.contains("/avisar")) {
			return Capacidad.NOTIFICACIONES_DISPARAR_AVISOS;
		}
		return Capacidad.NOTIFICACIONES_ENVIAR;
	}

	/** Rutas propias del usuario (su perfil, sus cosas): nunca se bloquean por permisos de gestión. */
	private static boolean esPropia(String ruta) {
		return ruta.contains("/me/") || ruta.endsWith("/me") || ruta.endsWith("/mios") || ruta.endsWith("/mias");
	}

	private static boolean en(String ruta, String prefijo) {
		return ruta.equals(prefijo) || ruta.startsWith(prefijo + "/");
	}

	private static boolean termina(String ruta, String sufijo) {
		return ruta.endsWith(sufijo);
	}
}
