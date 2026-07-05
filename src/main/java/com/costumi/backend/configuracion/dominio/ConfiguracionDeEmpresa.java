package com.costumi.backend.configuracion.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuración de una Empresa (RF-12.4): interruptores de módulos que materializan el principio
 * de configurabilidad por local. Valores por defecto sensatos; lo avanzado se activa a pedido.
 */
public class ConfiguracionDeEmpresa {

	private final UUID empresaId;
	private final boolean conteoStock;
	private final boolean multasActivo;
	private final boolean multiSucursal;
	private final boolean pagoEnLinea;

	private ConfiguracionDeEmpresa(UUID empresaId, boolean conteoStock, boolean multasActivo, boolean multiSucursal,
			boolean pagoEnLinea) {
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.conteoStock = conteoStock;
		this.multasActivo = multasActivo;
		this.multiSucursal = multiSucursal;
		this.pagoEnLinea = pagoEnLinea;
	}

	/** Defaults sensatos (RF-1, RF-13.5): conteo y multas activos; multi-sucursal y pago en línea apagados. */
	public static ConfiguracionDeEmpresa porDefecto(UUID empresaId) {
		return new ConfiguracionDeEmpresa(empresaId, true, true, false, false);
	}

	public static ConfiguracionDeEmpresa de(UUID empresaId, boolean conteoStock, boolean multasActivo,
			boolean multiSucursal, boolean pagoEnLinea) {
		return new ConfiguracionDeEmpresa(empresaId, conteoStock, multasActivo, multiSucursal, pagoEnLinea);
	}

	public UUID empresaId() {
		return empresaId;
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
