package com.costumi.backend.devoluciones.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.devoluciones.dominio.EstadoPieza;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de un ítem del checklist de devolución. */
@Entity
@Table(name = "pieza_revisada")
@Filter(name = FiltroTenant.NOMBRE)
class PiezaRevisadaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "devolucion_id", nullable = false)
	private UUID devolucionId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@Column(nullable = false, length = 200)
	private String descripcion;

	@Column(nullable = false)
	private boolean llego;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoPieza estado;

	@Column(name = "perdida_cobrada", nullable = false)
	private boolean perdidaCobrada;

	protected PiezaRevisadaJpaEntity() {
		// requerido por JPA
	}

	PiezaRevisadaJpaEntity(UUID id, UUID devolucionId, UUID empresaId, UUID prendaId, String descripcion, boolean llego,
			EstadoPieza estado, boolean perdidaCobrada) {
		this.id = id;
		this.devolucionId = devolucionId;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.descripcion = descripcion;
		this.llego = llego;
		this.estado = estado;
		this.perdidaCobrada = perdidaCobrada;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	boolean isPerdidaCobrada() {
		return perdidaCobrada;
	}

	String getDescripcion() {
		return descripcion;
	}

	boolean isLlego() {
		return llego;
	}

	EstadoPieza getEstado() {
		return estado;
	}
}
