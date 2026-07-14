package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/** Mapeo JPA de la plantilla de notificación. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "plantilla_notificacion")
@Filter(name = FiltroTenant.NOMBRE)
class PlantillaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoDeEvento tipo;

	@Column(nullable = false, length = 1000)
	private String texto;

	@Column(nullable = false)
	private boolean activa;

	protected PlantillaJpaEntity() {
		// requerido por JPA
	}

	PlantillaJpaEntity(UUID id, UUID empresaId, TipoDeEvento tipo, String texto, boolean activa) {
		this.id = id;
		this.empresaId = empresaId;
		this.tipo = tipo;
		this.texto = texto;
		this.activa = activa;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	TipoDeEvento getTipo() {
		return tipo;
	}

	String getTexto() {
		return texto;
	}

	boolean isActiva() {
		return activa;
	}
}
