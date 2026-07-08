package com.costumi.backend.disfraces.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos para rentar un disfraz. Las {@code selecciones} indican, para los slots personalizables (y los
 * opcionales que el cliente decide incluir), qué prenda elige por número de orden del slot.
 */
public record RentarDisfrazComando(UUID empresaId, UUID disfrazId, UUID sucursalId, UUID clienteId,
		LocalDate fechaRetiro, LocalDate fechaDevolucion, List<SeleccionDeSlot> selecciones, UUID empleadoId) {

	/** Elección del cliente para un slot: su número de orden y la prenda elegida (para personalizables). */
	public record SeleccionDeSlot(int orden, UUID prendaId) {
	}
}
