package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.Pago;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista los Pagos de un concepto (renta/venta), scoped por tenant. */
public interface ConsultarPagos {

	List<Pago> deConcepto(UUID empresaId, UUID conceptoId);
}
