package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.aplicacion.ConsultarPagos;
import com.costumi.backend.pagos.aplicacion.RegistrarCobroMixto;
import com.costumi.backend.pagos.aplicacion.RegistrarCobroMixtoComando;
import com.costumi.backend.pagos.aplicacion.RegistrarPago;
import com.costumi.backend.pagos.aplicacion.RegistrarPagoComando;
import com.costumi.backend.pagos.aplicacion.ResultadoCobroMixto;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PorcionDePago;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Pagos ligados a renta/venta (RF-6), acotados al tenant. El empleado sale del token. */
@RestController
@RequestMapping("/api/v1/pagos")
class PagoController {

	private final RegistrarPago registrarPago;
	private final ConsultarPagos consultarPagos;
	private final RegistrarCobroMixto registrarCobroMixto;

	PagoController(RegistrarPago registrarPago, ConsultarPagos consultarPagos, RegistrarCobroMixto registrarCobroMixto) {
		this.registrarPago = registrarPago;
		this.consultarPagos = consultarPagos;
		this.registrarCobroMixto = registrarCobroMixto;
	}

	@PostMapping
	ResponseEntity<PagoResponse> registrar(@Valid @RequestBody RegistrarPagoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID empleadoId = UUID.fromString(jwt.getSubject());
		Pago pago = registrarPago.ejecutar(new RegistrarPagoComando(empresaId, request.sucursalId(), empleadoId,
				request.tipoConcepto(), request.conceptoId(), request.monto(), request.tipoPago(), request.metodo(),
				request.referencia(), request.claveIdempotencia()));
		URI location = uriBuilder.path("/api/v1/pagos/{id}").buildAndExpand(pago.id()).toUri();
		return ResponseEntity.created(location).body(PagoResponse.desde(pago));
	}

	@PostMapping("/mixto")
	ResponseEntity<CobroMixtoResponse> registrarMixto(@Valid @RequestBody RegistrarCobroMixtoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID empleadoId = UUID.fromString(jwt.getSubject());
		List<PorcionDePago> porciones = request.porciones().stream()
				.map(p -> new PorcionDePago(p.metodo(), p.monto(), p.referencia())).toList();
		ResultadoCobroMixto resultado = registrarCobroMixto.ejecutar(new RegistrarCobroMixtoComando(empresaId,
				request.sucursalId(), empleadoId, request.tipoConcepto(), request.conceptoId(), porciones,
				request.efectivoRecibido(), request.claveIdempotencia()));
		return ResponseEntity.status(201).body(CobroMixtoResponse.desde(resultado));
	}

	@GetMapping
	List<PagoResponse> listar(@RequestParam UUID conceptoId, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarPagos.deConcepto(UUID.fromString(empresaId), conceptoId).stream()
				.map(PagoResponse::desde).toList();
	}

	@GetMapping("/saldo")
	SaldoResponse saldo(@RequestParam UUID conceptoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return new SaldoResponse(conceptoId, consultarPagos.saldoNeto(empresaId, conceptoId));
	}

	@GetMapping("/deposito")
	com.costumi.backend.pagos.aplicacion.EstadoDeposito deposito(@RequestParam UUID conceptoId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarPagos.estadoDeposito(empresaId, conceptoId);
	}

	@GetMapping("/comprobante")
	ComprobanteResponse comprobante(@RequestParam UUID conceptoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return ComprobanteResponse.desde(consultarPagos.comprobante(empresaId, conceptoId));
	}

	record SaldoResponse(UUID conceptoId, java.math.BigDecimal saldoNeto) {
	}
}
