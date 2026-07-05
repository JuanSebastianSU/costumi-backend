package com.costumi.backend.catalogo.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA del Valor de etiqueta. Lleva {@code empresa_id} (tenant) y su tipo. */
@Entity
@Table(name = "valor_etiqueta")
@Filter(name = FiltroTenant.NOMBRE)
class ValorEtiquetaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "tipo_etiqueta_id", nullable = false)
	private UUID tipoEtiquetaId;

	@Column(nullable = false, length = 120)
	private String valor;

	@Column(nullable = false)
	private boolean archivada;

	protected ValorEtiquetaJpaEntity() {
		// requerido por JPA
	}

	ValorEtiquetaJpaEntity(UUID id, UUID empresaId, UUID tipoEtiquetaId, String valor, boolean archivada) {
		this.id = id;
		this.empresaId = empresaId;
		this.tipoEtiquetaId = tipoEtiquetaId;
		this.valor = valor;
		this.archivada = archivada;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getTipoEtiquetaId() {
		return tipoEtiquetaId;
	}

	String getValor() {
		return valor;
	}

	boolean isArchivada() {
		return archivada;
	}
}
