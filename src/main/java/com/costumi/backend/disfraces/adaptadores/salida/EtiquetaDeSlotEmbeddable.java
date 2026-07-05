package com.costumi.backend.disfraces.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/** Un valor de etiqueta permitido en el pool de un slot: {@code tipo -> valor}. */
@Embeddable
class EtiquetaDeSlotEmbeddable {

	@Column(name = "tipo_etiqueta_id", nullable = false)
	private UUID tipoEtiquetaId;

	@Column(name = "valor_etiqueta_id", nullable = false)
	private UUID valorEtiquetaId;

	protected EtiquetaDeSlotEmbeddable() {
		// requerido por JPA
	}

	EtiquetaDeSlotEmbeddable(UUID tipoEtiquetaId, UUID valorEtiquetaId) {
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
