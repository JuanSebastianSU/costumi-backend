package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.rentas.dominio.EstadoRenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Mapeo JPA de la Renta. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "renta")
@Filter(name = FiltroTenant.NOMBRE)
class RentaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "sucursal_id", nullable = false)
	private UUID sucursalId;

	@Column(name = "cliente_id", nullable = false)
	private UUID clienteId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@Column(name = "fecha_retiro", nullable = false)
	private LocalDate fechaRetiro;

	@Column(name = "fecha_devolucion", nullable = false)
	private LocalDate fechaDevolucion;

	@Column(name = "precio_por_dia", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioPorDia;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal deposito;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal importe;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoRenta estado;

	@Column(name = "clave_idempotencia", length = 120)
	private String claveIdempotencia;

	@Column(name = "empleado_id")
	private UUID empleadoId;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "entregada_en")
	private Instant entregadaEn;

	@Column(name = "devuelta_real_en")
	private Instant devueltaRealEn;

	@Column(name = "cerrada_en")
	private Instant cerradaEn;

	protected RentaJpaEntity() {
		// requerido por JPA
	}

	RentaJpaEntity(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito, BigDecimal importe,
			EstadoRenta estado, String claveIdempotencia, UUID empleadoId, Instant creadaEn, Instant entregadaEn,
			Instant devueltaRealEn, Instant cerradaEn) {
		this.id = id;
		this.empresaId = empresaId;
		this.sucursalId = sucursalId;
		this.clienteId = clienteId;
		this.prendaId = prendaId;
		this.fechaRetiro = fechaRetiro;
		this.fechaDevolucion = fechaDevolucion;
		this.precioPorDia = precioPorDia;
		this.deposito = deposito;
		this.importe = importe;
		this.estado = estado;
		this.claveIdempotencia = claveIdempotencia;
		this.empleadoId = empleadoId;
		this.creadaEn = creadaEn;
		this.entregadaEn = entregadaEn;
		this.devueltaRealEn = devueltaRealEn;
		this.cerradaEn = cerradaEn;
	}

	String getClaveIdempotencia() {
		return claveIdempotencia;
	}

	UUID getEmpleadoId() {
		return empleadoId;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getSucursalId() {
		return sucursalId;
	}

	UUID getClienteId() {
		return clienteId;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	LocalDate getFechaRetiro() {
		return fechaRetiro;
	}

	LocalDate getFechaDevolucion() {
		return fechaDevolucion;
	}

	BigDecimal getPrecioPorDia() {
		return precioPorDia;
	}

	BigDecimal getDeposito() {
		return deposito;
	}

	BigDecimal getImporte() {
		return importe;
	}

	EstadoRenta getEstado() {
		return estado;
	}

	Instant getCreadaEn() {
		return creadaEn;
	}

	Instant getEntregadaEn() {
		return entregadaEn;
	}

	Instant getDevueltaRealEn() {
		return devueltaRealEn;
	}

	Instant getCerradaEn() {
		return cerradaEn;
	}
}
