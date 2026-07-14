package com.costumi.backend.identidad;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública de Identidad para que otros módulos <b>validen referencias a una Sucursal</b> antes de
 * anclar operaciones a ella (stock, rentas, ventas, caja, pedidos), sin conocer sus clases internas.
 *
 * <p>Cierra el hueco SEC-1 del barrido de seguridad: sin esta validación se podía crear stock/renta/venta/
 * turno apuntando a una sucursal inexistente, de otra empresa o archivada (referencia colgante). La
 * dirección {@code inventario/rentas/ventas/caja/pedidos → identidad} es acíclica (Identidad solo depende
 * de {@code configuracion}).
 */
public interface ConsultaDeSucursales {

	/**
	 * ¿La sucursal existe, pertenece a la empresa (tenant) y está <b>activa</b> (no archivada)? Valida por
	 * {@code empresaId} explícito, por lo que también es correcta cuando el filtro multi-tenant no aplica
	 * (p. ej. el CLIENTE del marketplace, cuyo token no lleva {@code empresa_id}).
	 */
	boolean existeActiva(UUID empresaId, UUID sucursalId);

	/** Dirección de texto y enlace de Google Maps de una sucursal del tenant (para {@code {direccion}}/{@code {maps}}
	 * en los mensajes automáticos, RF-11). Vacío si la sucursal no existe o no es de la empresa. */
	Optional<UbicacionDeSucursal> ubicacion(UUID empresaId, UUID sucursalId);

	/** Datos de ubicación de una sucursal para los mensajes; cualquiera de los dos puede ser null. */
	record UbicacionDeSucursal(String direccion, String ubicacionMaps) {
	}
}
