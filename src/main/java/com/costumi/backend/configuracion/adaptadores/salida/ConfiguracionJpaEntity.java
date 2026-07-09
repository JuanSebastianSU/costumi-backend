package com.costumi.backend.configuracion.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import com.costumi.backend.configuracion.dominio.RecargoPorRetraso;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de la configuración; la PK es {@code empresa_id} (una por empresa). */
@Entity
@Table(name = "configuracion_empresa")
@Filter(name = FiltroTenant.NOMBRE)
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

	@Column(name = "tasa_impuesto", nullable = false, precision = 5, scale = 4)
	private BigDecimal tasaImpuesto;

	@Column(name = "moneda", nullable = false, length = 3)
	private String moneda;

	@Column(name = "recargo_retraso_dia", nullable = false, precision = 12, scale = 2)
	private BigDecimal recargoPorRetrasoPorDia;

	@Enumerated(EnumType.STRING)
	@Column(name = "modo_recargo_retraso", nullable = false, length = 12)
	private RecargoPorRetraso modoRecargoRetraso;

	protected ConfiguracionJpaEntity() {
		// requerido por JPA
	}

	ConfiguracionJpaEntity(UUID empresaId, boolean conteoStock, boolean multasActivo, boolean multiSucursal,
			boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda, BigDecimal recargoPorRetrasoPorDia,
			RecargoPorRetraso modoRecargoRetraso) {
		this.empresaId = empresaId;
		this.conteoStock = conteoStock;
		this.multasActivo = multasActivo;
		this.multiSucursal = multiSucursal;
		this.pagoEnLinea = pagoEnLinea;
		this.tasaImpuesto = tasaImpuesto;
		this.moneda = moneda;
		this.recargoPorRetrasoPorDia = recargoPorRetrasoPorDia;
		this.modoRecargoRetraso = modoRecargoRetraso;
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

	BigDecimal getTasaImpuesto() {
		return tasaImpuesto;
	}

	String getMoneda() {
		return moneda;
	}

	BigDecimal getRecargoPorRetrasoPorDia() {
		return recargoPorRetrasoPorDia;
	}

	RecargoPorRetraso getModoRecargoRetraso() {
		return modoRecargoRetraso;
	}
}
