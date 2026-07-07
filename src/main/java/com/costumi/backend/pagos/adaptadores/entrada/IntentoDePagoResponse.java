package com.costumi.backend.pagos.adaptadores.entrada;

import java.util.UUID;

/** DTO de salida del intento de pago en línea: el id del intento y la URL de checkout de la pasarela. */
public record IntentoDePagoResponse(UUID intentoId, String urlCheckout) {
}
