package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.aplicacion.ResultadoCobroMixto;

import java.math.BigDecimal;
import java.util.List;

/** DTO de salida de un cobro mixto: los pagos generados (uno por método), el total y el vuelto (RF-6.7). */
public record CobroMixtoResponse(List<PagoResponse> pagos, BigDecimal total, BigDecimal vuelto) {

	static CobroMixtoResponse desde(ResultadoCobroMixto r) {
		return new CobroMixtoResponse(r.pagos().stream().map(PagoResponse::desde).toList(), r.total(), r.vuelto());
	}
}
