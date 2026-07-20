package com.costumi.backend.rentas.aplicacion;

/**
 * Puerto de entrada: cancela las reservas (rentas RESERVADA) que a las 24 h de creadas siguen sin pagar.
 * Libera el calendario de la reserva en efectivo no retirada y de la de tarjeta no pagada.
 */
public interface ExpirarReservas {

	/** Cancela las reservas vencidas y devuelve cuántas canceló. */
	int ejecutar();
}
