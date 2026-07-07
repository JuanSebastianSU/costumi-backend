package com.costumi.backend.configuracion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * API pública de Configuración para que otros módulos consulten los interruptores de la empresa y
 * <b>los respeten de verdad</b> (RF-12.4). P. ej. Notificaciones no avisa multas si el módulo de
 * multas está apagado (RF-6.6); Pagos usa la tasa de impuesto para el desglose del comprobante (RF-6.5).
 */
public interface ConsultaDeConfiguracion {

	/** ¿El módulo de multas está activo para la empresa? (por defecto sí). */
	boolean multasActivas(UUID empresaId);

	/** ¿La empresa cuenta stock? (por defecto sí; si no, venta/renta no controlan inventario, RF-12.4). */
	boolean conteoStock(UUID empresaId);

	/** ¿La empresa tiene habilitado multi-sucursal? (por defecto no → una sola sucursal, RF-12.4). */
	boolean multiSucursal(UUID empresaId);

	/** Tasa de impuesto de la empresa en [0, 1) (por defecto 0); precios impuesto-incluido (RF-6.5/12.2). */
	BigDecimal tasaImpuesto(UUID empresaId);

	/** ¿La empresa acepta pago en línea (pasarela)? (por defecto no, RF-6.11/12.4). */
	boolean pagoEnLinea(UUID empresaId);
}
