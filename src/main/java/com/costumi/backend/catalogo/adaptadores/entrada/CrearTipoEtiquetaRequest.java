package com.costumi.backend.catalogo.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear un Tipo de etiqueta. {@code categoriasQueAplica} vacío/ausente = el tipo
 * aplica a todas las categorías (RF-2.7.2).
 */
public record CrearTipoEtiquetaRequest(

		@NotBlank(message = "El nombre del tipo de etiqueta es obligatorio")
		@Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
		String nombre,

		boolean defineVariante,

		boolean seleccionablePorCliente,

		List<UUID> categoriasQueAplica) {

	public CrearTipoEtiquetaRequest {
		categoriasQueAplica = (categoriasQueAplica == null) ? List.of() : categoriasQueAplica;
	}
}
