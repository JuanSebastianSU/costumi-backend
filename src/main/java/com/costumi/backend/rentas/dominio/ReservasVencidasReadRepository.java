package com.costumi.backend.rentas.dominio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida (modelo de lectura sobre el esquema compartido): reservas a expirar. Una reserva
 * (renta RESERVADA) que a las 24 h de creada sigue <b>sin pagar</b> se cancela; así se libera el
 * calendario tanto de la reserva en efectivo no retirada como de la de tarjeta no pagada.
 */
public interface ReservasVencidasReadRepository {

	/** Una reserva a expirar, con su empresa (para cancelarla acotada al tenant). */
	record ReservaVencida(UUID empresaId, UUID rentaId) {
	}

	/** Rentas RESERVADA creadas antes de {@code limite} cuyo importe aún no está cubierto por los pagos. */
	List<ReservaVencida> reservasVencidas(Instant limite);
}
