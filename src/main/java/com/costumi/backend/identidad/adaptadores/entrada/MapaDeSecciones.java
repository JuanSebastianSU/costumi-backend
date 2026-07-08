package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Permiso;
import com.costumi.backend.identidad.dominio.Seccion;

import java.util.Optional;

/**
 * Mapea un request (método + ruta) a la {@link Seccion} y {@link AccionDePermiso} que exige, para el
 * chequeo de permisos granulares (RF-1.5). Las rutas no mapeadas no llevan chequeo granular (las cubre
 * la autorización por rol de {@code SecurityConfig}). GET/HEAD = ver; el resto = actuar.
 */
final class MapaDeSecciones {

	private MapaDeSecciones() {
	}

	static Optional<Permiso> permisoRequerido(String metodo, String ruta) {
		Seccion seccion = seccionDe(ruta);
		if (seccion == null) {
			return Optional.empty();
		}
		AccionDePermiso accion = ("GET".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo))
				? AccionDePermiso.VER
				: AccionDePermiso.ACCION;
		return Optional.of(new Permiso(seccion, accion));
	}

	private static Seccion seccionDe(String ruta) {
		if (empiezaCon(ruta, "/api/v1/prendas") || empiezaCon(ruta, "/api/v1/grupos-stock")) {
			return Seccion.INVENTARIO;
		}
		if (empiezaCon(ruta, "/api/v1/disfraces")) {
			return Seccion.DISFRACES;
		}
		if (empiezaCon(ruta, "/api/v1/ventas")) {
			return Seccion.VENTAS;
		}
		if (empiezaCon(ruta, "/api/v1/rentas")) {
			return Seccion.RENTAS;
		}
		if (empiezaCon(ruta, "/api/v1/devoluciones")) {
			return Seccion.DEVOLUCIONES;
		}
		if (empiezaCon(ruta, "/api/v1/pagos")) {
			return Seccion.PAGOS;
		}
		if (empiezaCon(ruta, "/api/v1/caja")) {
			return Seccion.CAJA;
		}
		if (empiezaCon(ruta, "/api/v1/reportes") || empiezaCon(ruta, "/api/v1/auditoria")) {
			return Seccion.REPORTES;
		}
		if (empiezaCon(ruta, "/api/v1/clientes")) {
			return Seccion.CLIENTES;
		}
		if (empiezaCon(ruta, "/api/v1/configuracion")) {
			return Seccion.CONFIGURACION;
		}
		if (empiezaCon(ruta, "/api/v1/notificaciones")) {
			return Seccion.NOTIFICACIONES;
		}
		if (empiezaCon(ruta, "/api/v1/empleados")) {
			return Seccion.EMPLEADOS;
		}
		return null;
	}

	private static boolean empiezaCon(String ruta, String prefijo) {
		return ruta.equals(prefijo) || ruta.startsWith(prefijo + "/");
	}
}
