package com.costumi.backend.configuracion.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuración de una Empresa (RF-12.4): interruptores de módulos que materializan el principio
 * de configurabilidad por local. Valores por defecto sensatos; lo avanzado se activa a pedido.
 * Incluye la <b>tasa de impuesto</b> configurable por empresa (RF-6.5/12.2), con precios
 * impuesto-incluido (el desglose se calcula en el comprobante).
 */
public class ConfiguracionDeEmpresa {

	private final UUID empresaId;
	private final boolean conteoStock;
	private final boolean multasActivo;
	private final boolean multiSucursal;
	private final boolean pagoEnLinea;
	private final BigDecimal tasaImpuesto;

	private ConfiguracionDeEmpresa(UUID empresaId, boolean conteoStock, boolean multasActivo, boolean multiSucursal,
			boolean pagoEnLinea, BigDecimal tasaImpuesto) {
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.conteoStock = conteoStock;
		this.multasActivo = multasActivo;
		this.multiSucursal = multiSucursal;
		this.pagoEnLinea = pagoEnLinea;
		Objects.requireNonNull(tasaImpuesto, "tasaImpuesto");
		if (tasaImpuesto.signum() < 0 || tasaImpuesto.compareTo(BigDecimal.ONE) >= 0) {
			throw new IllegalArgumentException("La tasa de impuesto debe estar en [0, 1) (p. ej. 0.19 = 19%)");
		}
		this.tasaImpuesto = tasaImpuesto;
	}

	/** Defaults sensatos (RF-1, RF-13.5): conteo y multas activos; multi-sucursal, pago en línea e impuesto en 0. */
	public static ConfiguracionDeEmpresa porDefecto(UUID empresaId) {
		return new ConfiguracionDeEmpresa(empresaId, true, true, false, false, BigDecimal.ZERO);
	}

	public static ConfiguracionDeEmpresa de(UUID empresaId, boolean conteoStock, boolean multasActivo,
			boolean multiSucursal, boolean pagoEnLinea, BigDecimal tasaImpuesto) {
		return new ConfiguracionDeEmpresa(empresaId, conteoStock, multasActivo, multiSucursal, pagoEnLinea,
				tasaImpuesto);
	}

	public UUID empresaId() {
		return empresaId;
	}

	public BigDecimal tasaImpuesto() {
		return tasaImpuesto;
	}

	public boolean conteoStock() {
		return conteoStock;
	}

	public boolean multasActivo() {
		return multasActivo;
	}

	public boolean multiSucursal() {
		return multiSucursal;
	}

	public boolean pagoEnLinea() {
		return pagoEnLinea;
	}
}
