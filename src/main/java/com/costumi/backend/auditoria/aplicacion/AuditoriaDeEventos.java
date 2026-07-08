package com.costumi.backend.auditoria.aplicacion;

import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.identidad.EmpresaAprobada;
import com.costumi.backend.identidad.EmpresaGestionada;
import com.costumi.backend.inventario.StockAjustado;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	void empresaAprobada(EmpresaAprobada evento) {
		auditoria.registrar(evento.empresaId(), "EMPRESA_APROBADA", "La empresa fue aprobada por el SuperAdmin");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	void empresaGestionada(EmpresaGestionada evento) {
		auditoria.registrar(evento.empresaId(), "EMPRESA_" + evento.accion(),
				"La empresa fue " + evento.accion().toLowerCase() + " por el SuperAdmin");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	void devolucionRegistrada(DevolucionRegistrada evento) {
		auditoria.registrar(evento.empresaId(), "DEVOLUCION_REGISTRADA",
				"Devolución " + evento.devolucionId() + " (renta " + evento.rentaId() + ", multa " + evento.multa() + ")");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	void stockAjustado(StockAjustado evento) {
		auditoria.registrar(evento.empresaId(), "STOCK_AJUSTADO", "Ajuste de " + evento.estado() + " en "
				+ evento.delta() + " (grupo " + evento.grupoId() + "): " + evento.motivo());
	}
}
