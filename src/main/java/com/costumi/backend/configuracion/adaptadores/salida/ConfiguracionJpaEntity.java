package com.costumi.backend.configuracion.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de la configuración; la PK es {@code empresa_id} (una por empresa). */
@Entity
@Table(name = "configuracion_empresa")
class ConfiguracionJpaEntity {

	@Id
	@Column(name = "empresa_id")
	private UUID empresaId;

	@Column(name = "conteo_stock", nullable = false)
	private boolean conteoStock;

	@Column(name = "multas_activo", nullable = false)
	private boolean multasActivo;

	@Column(name = "multi_sucursal", nullable = false)
	private boolean multiSucursal;

	@Column(name = "pago_en_linea", nullable = false)
	private boolean pagoEnLinea;

	protected ConfiguracionJpaEntity() {
		// requerido por JPA
	}

	ConfiguracionJpaEntity(UUID empresaId, boolean conteoStock, boolean multasActivo, boolean multiSucursal,
			boolean pagoEnLinea) {
		this.empresaId = empresaId;
		this.conteoStock = conteoStock;
		this.multasActivo = multasActivo;
		this.multiSucursal = multiSucursal;
		this.pagoEnLinea = pagoEnLinea;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	boolean isConteoStock() {
		return conteoStock;
	}

	boolean isMultasActivo() {
		return multasActivo;
	}

	boolean isMultiSucursal() {
		return multiSucursal;
	}

	boolean isPagoEnLinea() {
		return pagoEnLinea;
	}
}
