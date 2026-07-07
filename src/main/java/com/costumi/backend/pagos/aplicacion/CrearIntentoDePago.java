package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.util.UUID;

/** Puerto de entrada: iniciar un pago en línea (crea el checkout en la pasarela) (RF-6.11). */
public interface CrearIntentoDePago {

	Resultado ejecutar(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto, UUID conceptoId,
			BigDecimal monto, String moneda);

	record Resultado(UUID intentoId, String urlCheckout) {
	}
}
