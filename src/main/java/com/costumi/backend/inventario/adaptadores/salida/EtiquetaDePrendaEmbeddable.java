package com.costumi.backend.inventario.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/** Una etiqueta de clasificación de una prenda: {@code tipo -> valor} de etiqueta. */
@Embeddable
class EtiquetaDePrendaEmbeddable {

	@Column(name = "tipo_etiqueta_id", nullable = false)
	private UUID tipoEtiquetaId;

	@Column(name = "valor_etiqueta_id", nullable = false)
	private UUID valorEtiquetaId;

	protected EtiquetaDePrendaEmbeddable() {
		// requerido por JPA
	}

	EtiquetaDePrendaEmbeddable(UUID tipoEtiquetaId, UUID valorEtiquetaId) {
		this.tipoEtiquetaId = tipoEtiquetaId;
		this.valorEtiquetaId = valorEtiquetaId;
	}

	UUID getTipoEtiquetaId() {
		return tipoEtiquetaId;
	}

	UUID getValorEtiquetaId() {
		return valorEtiquetaId;
	}
}
