package com.costumi.backend.inventario.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/** Una fila de la combinación de variante de un grupo de stock: {@code tipo -> valor} de etiqueta. */
@Embeddable
class ValorDeVarianteEmbeddable {

	@Column(name = "tipo_etiqueta_id", nullable = false)
	private UUID tipoEtiquetaId;

	@Column(name = "valor_etiqueta_id", nullable = false)
	private UUID valorEtiquetaId;

	protected ValorDeVarianteEmbeddable() {
		// requerido por JPA
	}

	ValorDeVarianteEmbeddable(UUID tipoEtiquetaId, UUID valorEtiquetaId) {
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
