package com.costumi.backend.auditoria.aplicacion;

import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.identidad.EmpresaAprobada;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Alimenta la auditoría (RF-0.5) a partir de los domain events (§5.5). A medida que más operaciones
 * publiquen eventos (venta, pago, caja…), se añaden aquí sus @EventListener sin tocar esos módulos.
 */
@Component
class AuditoriaDeEventos {

	private final RegistrarAuditoria auditoria;

	AuditoriaDeEventos(RegistrarAuditoria auditoria) {
		this.auditoria = auditoria;
	}

	@EventListener
	void empresaAprobada(EmpresaAprobada evento) {
		auditoria.registrar(evento.empresaId(), "EMPRESA_APROBADA", "La empresa fue aprobada por el SuperAdmin");
	}

	@EventListener
	void devolucionRegistrada(DevolucionRegistrada evento) {
		auditoria.registrar(evento.empresaId(), "DEVOLUCION_REGISTRADA",
				"Devolución " + evento.devolucionId() + " (renta " + evento.rentaId() + ", multa " + evento.multa() + ")");
	}
}
