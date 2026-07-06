package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de Pagos, acotados a la empresa (tenant), con idempotencia opcional. */
@Service
class PagoService implements RegistrarPago, ConsultarPagos {

	private final PagoRepository pagos;

	PagoService(PagoRepository pagos) {
		this.pagos = pagos;
	}

	@Override
	@Transactional
	public Pago ejecutar(RegistrarPagoComando comando) {
		if (comando.claveIdempotencia() != null && !comando.claveIdempotencia().isBlank()) {
			Optional<Pago> existente = pagos.buscarPorClave(comando.empresaId(), comando.claveIdempotencia().trim());
			if (existente.isPresent()) {
				return existente.get(); // idempotente: no se duplica el cobro
			}
		}
		return pagos.guardar(Pago.registrar(comando.empresaId(), comando.sucursalId(), comando.empleadoId(),
				comando.tipoConcepto(), comando.conceptoId(), comando.monto(), comando.tipoPago(), comando.metodo(),
				comando.referencia(), comando.claveIdempotencia()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Pago> deConcepto(UUID empresaId, UUID conceptoId) {
		return pagos.listarPorConcepto(empresaId, conceptoId);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal saldoNeto(UUID empresaId, UUID conceptoId) {
		return pagos.saldoNetoPorConcepto(empresaId, conceptoId);
	}
}
