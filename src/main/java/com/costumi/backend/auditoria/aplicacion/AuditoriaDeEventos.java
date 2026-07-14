package com.costumi.backend.auditoria.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.identidad.EmpresaAprobada;
import com.costumi.backend.identidad.EmpresaGestionada;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.inventario.StockAjustado;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Alimenta la auditoría (RF-0.5) a partir de los domain events (§5.5). A medida que más operaciones
 * publiquen eventos (venta, pago, caja…), se añaden aquí sus @EventListener sin tocar esos módulos.
 *
 * <p>El detalle se escribe <b>legible</b>: resuelve el nombre de la prenda/cliente vía las APIs públicas
 * de Inventario/Clientes (en vez de dejar ids opacos), tomando el nombre vigente al momento del evento.
 */
@Component
class AuditoriaDeEventos {

	private final RegistrarAuditoria auditoria;
	private final ConsultaDeInventario inventario;
	private final ResolucionDeClientes clientes;

	AuditoriaDeEventos(RegistrarAuditoria auditoria, ConsultaDeInventario inventario,
			ResolucionDeClientes clientes) {
		this.auditoria = auditoria;
		this.inventario = inventario;
		this.clientes = clientes;
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
		String cliente = clientes.nombreDeCliente(evento.empresaId(), evento.clienteId()).orElse(null);
		String quien = cliente == null ? "un cliente" : "«" + cliente + "»";
		auditoria.registrar(evento.empresaId(), "DEVOLUCION_REGISTRADA",
				"Devolución de " + quien + " — multa " + dinero(evento.multa()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	void stockAjustado(StockAjustado evento) {
		String prenda = inventario.opcionDePrenda(evento.empresaId(), evento.prendaId())
				.map(ConsultaDeInventario.OpcionDePool::nombre)
				.orElse("una prenda");
		auditoria.registrar(evento.empresaId(), "STOCK_AJUSTADO",
				"Ajuste de " + evento.estado() + " (" + conSigno(evento.delta()) + ") en «" + prenda + "»: "
						+ evento.motivo());
	}

	/** Formato de dinero simple para el trail (el importe exacto ya vive en Pagos). */
	private static String dinero(java.math.BigDecimal monto) {
		return monto == null ? "$0" : "$" + monto.stripTrailingZeros().toPlainString();
	}

	private static String conSigno(int delta) {
		return delta >= 0 ? "+" + delta : String.valueOf(delta);
	}
}
