package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.identidad.ConsultaDeJerarquiaDeRoles;
import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PagoRepository;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolsoRepository;
import com.costumi.backend.pagos.dominio.TipoPago;
import com.costumi.backend.rentas.ConsultaDeRentas;
import com.costumi.backend.ventas.ConsultaDeVentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Proceso de reembolso en dos pasos (RF-4.5/6.9). Solicitar valida saldo y evita duplicados; decidir aplica
 * el escalamiento por pirámide, exige que el ítem esté devuelto para aprobar, y al aprobar ejecuta el
 * reembolso (asiento REEMBOLSO idempotente + refund a la tarjeta si el cobro original fue con tarjeta y la
 * pasarela está configurada, P-6). El dinero solo se mueve al aprobar, nunca antes de recuperar la mercancía.
 */
@Service
class ReembolsoService implements SolicitarReembolso, DecidirReembolso, ConsultarReembolsos {

	private static final String PREFIJO_PASARELA = "pasarela:";

	private final SolicitudDeReembolsoRepository solicitudes;
	private final PagoRepository pagos;
	private final RegistrarPago registrarPago;
	private final PasarelaDePago pasarela;
	private final ConsultaDeVentas ventas;
	private final ConsultaDeRentas rentas;
	private final ConsultaDeJerarquiaDeRoles jerarquia;

	ReembolsoService(SolicitudDeReembolsoRepository solicitudes, PagoRepository pagos, RegistrarPago registrarPago,
			PasarelaDePago pasarela, ConsultaDeVentas ventas, ConsultaDeRentas rentas,
			ConsultaDeJerarquiaDeRoles jerarquia) {
		this.solicitudes = solicitudes;
		this.pagos = pagos;
		this.registrarPago = registrarPago;
		this.pasarela = pasarela;
		this.ventas = ventas;
		this.rentas = rentas;
		this.jerarquia = jerarquia;
	}

	@Override
	@Transactional
	public SolicitudDeReembolso ejecutar(SolicitarReembolsoComando comando) {
		BigDecimal saldo = pagos.saldoNetoPorConcepto(comando.empresaId(), comando.conceptoId());
		if (saldo.signum() <= 0) {
			throw new SolicitudDeReembolsoInvalida("La operación no tiene saldo pagado para reembolsar");
		}
		if (comando.monto().compareTo(saldo) > 0) {
			throw new SolicitudDeReembolsoInvalida(
					"El monto (" + comando.monto() + ") supera el saldo pagado (" + saldo + ")");
		}
		if (solicitudes.existePendientePorConcepto(comando.empresaId(), comando.conceptoId())) {
			throw new SolicitudDeReembolsoInvalida("Ya hay una solicitud de reembolso pendiente para esta operación");
		}
		return solicitudes.guardar(SolicitudDeReembolso.crear(comando.empresaId(), comando.tipoConcepto(),
				comando.conceptoId(), comando.solicitanteClienteId(), comando.monto(), comando.motivo()));
	}

	@Override
	@Transactional
	public SolicitudDeReembolso ejecutar(DecidirReembolsoComando comando) {
		SolicitudDeReembolso solicitud = solicitudes.buscarPorId(comando.solicitudId())
				.filter(s -> s.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new SolicitudDeReembolsoNoEncontrada(comando.solicitudId()));
		if (!solicitud.estaPendiente() && !solicitud.estaRechazada()) {
			throw new SolicitudDeReembolsoInvalida("La solicitud ya fue aprobada");
		}
		// Escalamiento: revertir una rechazada exige un rol superior al que la rechazó (pirámide B3).
		if (solicitud.estaRechazada() && !jerarquia.superaEstrictamente(comando.actorRol(), solicitud.rolDecision())) {
			throw new DecisionDeReembolsoNoPermitida();
		}
		// Precondición para aprobar: el ítem ya debe estar devuelto (el dinero sigue a la mercancía).
		if (comando.aprobar() && !itemDevuelto(solicitud)) {
			throw new ItemNoDevuelto();
		}
		solicitud.decidir(comando.aprobar(), comando.actorUsuarioId(), comando.actorRol(), comando.motivo());
		if (comando.aprobar()) {
			ejecutarReembolso(solicitud, comando.actorUsuarioId());
		}
		return solicitudes.guardar(solicitud);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SolicitudDeReembolso> deEmpresa(UUID empresaId) {
		return solicitudes.listarPorEmpresa(empresaId);
	}

	private boolean itemDevuelto(SolicitudDeReembolso solicitud) {
		return switch (solicitud.tipoConcepto()) {
			case VENTA -> ventas.estaDevuelta(solicitud.empresaId(), solicitud.conceptoId());
			case RENTA -> rentas.estaDevuelta(solicitud.empresaId(), solicitud.conceptoId());
		};
	}

	/**
	 * Al aprobar: registra el asiento REEMBOLSO (idempotente por solicitud) y, si el cobro original fue con
	 * tarjeta y la pasarela está configurada, dispara el refund real en el proveedor (P-6). Si la pasarela no
	 * está configurada (dev/test) queda solo el asiento — listo para credenciales.
	 */
	private void ejecutarReembolso(SolicitudDeReembolso solicitud, UUID actorUsuarioId) {
		List<Pago> delConcepto = pagos.listarPorConcepto(solicitud.empresaId(), solicitud.conceptoId());
		Optional<String> idExternoTarjeta = delConcepto.stream()
				.filter(p -> p.tipoPago() == TipoPago.COBRO && p.metodo() == MetodoPago.TARJETA
						&& p.claveIdempotencia() != null && p.claveIdempotencia().startsWith(PREFIJO_PASARELA))
				.map(p -> p.claveIdempotencia().substring(PREFIJO_PASARELA.length()))
				.findFirst();
		boolean porTarjeta = idExternoTarjeta.isPresent() && pasarela.configurada();
		UUID sucursalId = delConcepto.stream().map(Pago::sucursalId).findFirst().orElse(null);
		String clave = "reembolso:" + solicitud.id();
		registrarPago.ejecutar(new RegistrarPagoComando(solicitud.empresaId(), sucursalId, actorUsuarioId,
				solicitud.tipoConcepto(), solicitud.conceptoId(), solicitud.monto(), TipoPago.REEMBOLSO,
				porTarjeta ? MetodoPago.TARJETA : MetodoPago.EFECTIVO, clave, clave));
		if (porTarjeta) {
			pasarela.reembolsar(idExternoTarjeta.get(), solicitud.monto());
		}
	}
}
