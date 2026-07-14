package com.costumi.backend.devoluciones;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * API pública de Devoluciones para otros módulos (§5.5): cuánta multa acumuló una renta por sus
 * devoluciones (RF-5.2). La usa Pagos para detectar cuándo el cliente terminó de saldar su deuda
 * (importe de la renta + multa) y avisarle (RF-11.1), sin conocer las clases internas de Devoluciones.
 */
public interface ConsultaDeMultas {

	/** Suma de las multas de todas las devoluciones de la renta (0 si no hubo multa o no existe). */
	BigDecimal totalMultaDeRenta(UUID empresaId, UUID rentaId);
}
