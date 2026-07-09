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
	private final String moneda;
	private final BigDecimal recargoPorRetrasoPorDia;
	private final RecargoPorRetraso modoRecargoRetraso;
	private final boolean reembolsosActivos;
	private final int ventanaReembolsoDias;

	private ConfiguracionDeEmpresa(UUID empresaId, boolean conteoStock, boolean multasActivo, boolean multiSucursal,
			boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda, BigDecimal recargoPorRetrasoPorDia,
			RecargoPorRetraso modoRecargoRetraso, boolean reembolsosActivos, int ventanaReembolsoDias) {
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
		if (moneda == null || moneda.isBlank()) {
			throw new IllegalArgumentException("La moneda es obligatoria (código, p. ej. COP)");
		}
		this.moneda = moneda.trim().toUpperCase();
		Objects.requireNonNull(recargoPorRetrasoPorDia, "recargoPorRetrasoPorDia");
		if (recargoPorRetrasoPorDia.signum() < 0) {
			throw new IllegalArgumentException("El recargo por retraso por día no puede ser negativo");
		}
		this.recargoPorRetrasoPorDia = recargoPorRetrasoPorDia;
		this.modoRecargoRetraso = (modoRecargoRetraso == null) ? RecargoPorRetraso.ACUMULATIVA : modoRecargoRetraso;
		this.reembolsosActivos = reembolsosActivos;
		if (ventanaReembolsoDias < 0) {
			throw new IllegalArgumentException("La ventana de reembolso (días) no puede ser negativa");
		}
		this.ventanaReembolsoDias = ventanaReembolsoDias;
	}

	/** Defaults sensatos (RF-1/12.2): conteo y multas activos; multi-sucursal/pago en línea/impuesto/recargo en 0; moneda COP; recargo acumulativo; reembolsos activos sin ventana. */
	public static ConfiguracionDeEmpresa porDefecto(UUID empresaId) {
		return new ConfiguracionDeEmpresa(empresaId, true, true, false, false, BigDecimal.ZERO, "COP", BigDecimal.ZERO,
				RecargoPorRetraso.ACUMULATIVA, true, 0);
	}

	public static ConfiguracionDeEmpresa de(UUID empresaId, boolean conteoStock, boolean multasActivo,
			boolean multiSucursal, boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda,
			BigDecimal recargoPorRetrasoPorDia, RecargoPorRetraso modoRecargoRetraso, boolean reembolsosActivos,
			int ventanaReembolsoDias) {
		return new ConfiguracionDeEmpresa(empresaId, conteoStock, multasActivo, multiSucursal, pagoEnLinea,
				tasaImpuesto, moneda, recargoPorRetrasoPorDia, modoRecargoRetraso, reembolsosActivos,
				ventanaReembolsoDias);
	}

	/** Overload sin política de reembolso: reembolsos activos y sin ventana (defaults). */
	public static ConfiguracionDeEmpresa de(UUID empresaId, boolean conteoStock, boolean multasActivo,
			boolean multiSucursal, boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda,
			BigDecimal recargoPorRetrasoPorDia, RecargoPorRetraso modoRecargoRetraso) {
		return de(empresaId, conteoStock, multasActivo, multiSucursal, pagoEnLinea, tasaImpuesto, moneda,
				recargoPorRetrasoPorDia, modoRecargoRetraso, true, 0);
	}

	/**
	 * Recargo por retraso a cobrar (RF-5.2) según el modo: {@code ACUMULATIVA} = monto × días de atraso;
	 * {@code FIJA} = monto único si hubo atraso. Sin atraso (días ≤ 0), 0.
	 */
	public BigDecimal recargoPorRetraso(long diasAtraso) {
		if (diasAtraso <= 0) {
			return BigDecimal.ZERO;
		}
		return switch (modoRecargoRetraso) {
			case FIJA -> recargoPorRetrasoPorDia;
			case ACUMULATIVA -> recargoPorRetrasoPorDia.multiply(BigDecimal.valueOf(diasAtraso));
		};
	}

	public UUID empresaId() {
		return empresaId;
	}

	public BigDecimal tasaImpuesto() {
		return tasaImpuesto;
	}

	public String moneda() {
		return moneda;
	}

	public BigDecimal recargoPorRetrasoPorDia() {
		return recargoPorRetrasoPorDia;
	}

	public RecargoPorRetraso modoRecargoRetraso() {
		return modoRecargoRetraso;
	}

	public boolean reembolsosActivos() {
		return reembolsosActivos;
	}

	/** Ventana de reembolso en días desde la venta; 0 = sin límite (RF-4.5). */
	public int ventanaReembolsoDias() {
		return ventanaReembolsoDias;
	}

	/**
	 * ¿Se permite reembolsar (devolver) una venta hecha hace {@code diasDesdeLaVenta} días? (RF-4.5):
	 * requiere reembolsos activos y, si hay ventana ({@code > 0}), estar dentro de ella.
	 */
	public boolean reembolsoPermitido(long diasDesdeLaVenta) {
		if (!reembolsosActivos) {
			return false;
		}
		return ventanaReembolsoDias == 0 || diasDesdeLaVenta <= ventanaReembolsoDias;
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
