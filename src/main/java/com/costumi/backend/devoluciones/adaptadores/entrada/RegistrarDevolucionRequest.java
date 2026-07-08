package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.devoluciones.dominio.EstadoPieza;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de entrada para registrar una Devolución con su checklist de piezas. */
public record RegistrarDevolucionRequest(

		@NotNull(message = "La renta es obligatoria") UUID rentaId,

		BigDecimal deposito,

		BigDecimal cargoPorDanos,

		BigDecimal cargoPorRetraso,

		@Valid List<PiezaRequest> piezas) {

	/** Un ítem del checklist: una unidad de una prenda de la renta, con su número/QR y su estado. */
	public record PiezaRequest(

			@NotNull(message = "La prenda de la pieza es obligatoria") UUID prendaId,

			@NotBlank(message = "La descripción de la pieza es obligatoria") String descripcion,

			boolean llego,

			@NotNull(message = "El estado de la pieza es obligatorio") EstadoPieza estado) {
	}
}
