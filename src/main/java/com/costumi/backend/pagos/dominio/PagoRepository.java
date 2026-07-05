package com.costumi.backend.pagos.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Pagos (scoped por tenant). */
public interface PagoRepository {

	Pago guardar(Pago pago);

	Optional<Pago> buscarPorId(UUID id);

	List<Pago> listarPorConcepto(UUID empresaId, UUID conceptoId);

	/** Para idempotencia: el pago con esa clave en la empresa, si existe (RF-17.6). */
	Optional<Pago> buscarPorClave(UUID empresaId, String claveIdempotencia);
}
