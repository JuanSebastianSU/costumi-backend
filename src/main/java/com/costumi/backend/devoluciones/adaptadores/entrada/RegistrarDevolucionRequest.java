package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.devoluciones.dominio.EstadoPieza;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para registrar una Devolución con su checklist de piezas. {@code cargoPorRetraso} es
 * opcional: si se omite, se deriva del recargo configurado por día × días de atraso (RF-12.2/5.2),
 * usando {@code fechaDevolucionReal} (por defecto, hoy).
 */
public record RegistrarDevolucionRequest(

		@NotNull(message = "La renta es obligatoria") UUID rentaId,

		BigDecimal deposito,

		BigDecimal cargoPorDanos,

		BigDecimal cargoPorRetraso,

		LocalDate fechaDevolucionReal,

		@Valid List<PiezaRequest> piezas) {

	/** Un ítem del checklist: una unidad de una prenda de la renta, con su número/QR y su estado. */
	public record PiezaRequest(

			@NotNull(message = "La prenda de la pieza es obligatoria") UUID prendaId,

			@NotBlank(message = "La descripción de la pieza es obligatoria") String descripcion,

			boolean llego,

			@NotNull(message = "El estado de la pieza es obligatorio") EstadoPieza estado,

			boolean perdidaCobrada) {
	}
}
