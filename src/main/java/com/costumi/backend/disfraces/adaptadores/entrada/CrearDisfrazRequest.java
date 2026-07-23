package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.costumi.backend.disfraces.dominio.TipoDeDisfraz;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear/editar un Disfraz: su nombre, su {@code categoriaId} (opcional, para
 * agruparlo/verlo por categoría), la lista de {@code slots} (1..8, fijos o personalizables) y, opcional,
 * un {@code precioRentaGeneral} por día que anula la suma por prendas.
 */
public record CrearDisfrazRequest(

		@NotBlank(message = "El nombre del disfraz es obligatorio")
		String nombre,

		UUID categoriaId,

		BigDecimal precioRentaGeneral,

		BigDecimal precioVentaGeneral,

		/**
		 * Para qué está disponible: RENTA, VENTA o AMBOS (lo decide el dueño).
		 *
		 * <p><b>Opcional a propósito</b>: si no viene, el tipo se <b>deriva de las piezas</b> (el disfraz
		 * sirve para lo que sirvan todas ellas). Antes se rellenaba aquí con AMBOS, que es la opción más
		 * exigente —obliga a que cada pieza sirva para renta y para venta—, así que quien no elegía nada
		 * recibía un error por una decisión que nunca tomó.
		 */
		TipoDeDisfraz tipo,

		@Valid
		List<SlotDto> slots) {

	public CrearDisfrazRequest {
		slots = (slots == null) ? List.of() : slots;
	}
}
