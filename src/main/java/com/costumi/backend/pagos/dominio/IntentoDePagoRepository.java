package com.costumi.backend.pagos.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de intentos de pago en línea (RF-6.11). */
public interface IntentoDePagoRepository {

	IntentoDePago guardar(IntentoDePago intento);

	Optional<IntentoDePago> buscarPorId(UUID id);
}
