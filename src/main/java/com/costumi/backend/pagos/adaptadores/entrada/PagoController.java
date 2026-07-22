package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.pagos.aplicacion.ConfirmarPagoEnLinea;
import com.costumi.backend.pagos.aplicacion.ConsultarPagos;
import com.costumi.backend.pagos.aplicacion.CrearIntentoDePago;
import com.costumi.backend.pagos.aplicacion.CrearIntentoDePagoDeCliente;
import com.costumi.backend.pagos.aplicacion.CrearIntentoDePagoDeClienteComando;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
	private final com.costumi.backend.compartido.GeneradorDePdf pdf;
	private final CrearIntentoDePago crearIntentoDePago;
	private final CrearIntentoDePagoDeCliente crearIntentoDePagoDeCliente;
	private final ConfirmarPagoEnLinea confirmarPagoEnLinea;
	private final VerificadorDeFirmaDeWebhook verificadorDeFirma;
	private final com.costumi.backend.devoluciones.ConsultaDeMultas multas;
	private final ContextoDeTenant tenant;

	PagoController(RegistrarPago registrarPago, ConsultarPagos consultarPagos, RegistrarCobroMixto registrarCobroMixto,
			com.costumi.backend.compartido.GeneradorDePdf pdf, CrearIntentoDePago crearIntentoDePago,
			CrearIntentoDePagoDeCliente crearIntentoDePagoDeCliente, ConfirmarPagoEnLinea confirmarPagoEnLinea,
			VerificadorDeFirmaDeWebhook verificadorDeFirma, com.costumi.backend.devoluciones.ConsultaDeMultas multas,
			ContextoDeTenant tenant) {
		this.registrarPago = registrarPago;
		this.consultarPagos = consultarPagos;
		this.registrarCobroMixto = registrarCobroMixto;
		this.pdf = pdf;
		this.crearIntentoDePago = crearIntentoDePago;
		this.crearIntentoDePagoDeCliente = crearIntentoDePagoDeCliente;
		this.confirmarPagoEnLinea = confirmarPagoEnLinea;
		this.verificadorDeFirma = verificadorDeFirma;
		this.multas = multas;
		this.tenant = tenant;
	}

	/** Inicia un pago en línea (RF-6.11): crea el checkout en la pasarela y devuelve la URL. */
	@PostMapping("/intento")
	IntentoDePagoResponse intento(@Valid @RequestBody IntentoDePagoRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		UUID empleadoId = UUID.fromString(jwt.getSubject());
		CrearIntentoDePago.Resultado r = crearIntentoDePago.ejecutar(empresaId, request.sucursalId(), empleadoId,
				request.tipoConcepto(), request.conceptoId(), request.monto(), request.moneda());
		return new IntentoDePagoResponse(r.intentoId(), r.urlCheckout());
	}

	/**
	 * Inicia un pago en línea que paga el propio CLIENTE del marketplace por su operación (RF-6.11/14.4). La
	 * empresa (tienda) la indica el cliente; su ficha y la propiedad de la venta/renta salen de su token. El
	 * backend valida que el monto cubra el total pendiente (pago total de golpe) y devuelve la URL del checkout.
	 */
	@PostMapping("/intento/cliente")
	IntentoDePagoResponse intentoDeCliente(@Valid @RequestBody IntentoDePagoDeClienteRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		CrearIntentoDePago.Resultado r = crearIntentoDePagoDeCliente.ejecutar(new CrearIntentoDePagoDeClienteComando(
				request.empresaId(), usuarioId, jwt.getClaimAsString("email"), request.sucursalId(),
				request.tipoConcepto(), request.conceptoId(), request.monto(), request.moneda()));
		return new IntentoDePagoResponse(r.intentoId(), r.urlCheckout());
	}

	/**
	 * Webhook de la pasarela (RF-6.11): confirma el pago y registra el Pago (idempotente). Público, pero
	 * <b>firmado</b> (SEC-5): se exige un header {@code X-Signature} = HMAC del contenido con el secreto
	 * compartido; sin firma válida se rechaza (401), y sin secreto configurado se rechaza (503, fail-closed).
	 */
	@PostMapping("/webhook")
	ResponseEntity<Void> webhook(@Valid @RequestBody WebhookPagoRequest request,
			@RequestHeader(value = "X-Signature", required = false) String firma) {
		verificadorDeFirma.exigirFirmaValida(request.intentoId() + ":" + request.idPagoExterno(), firma);
		confirmarPagoEnLinea.ejecutar(request.intentoId(), request.idPagoExterno());
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	ResponseEntity<PagoResponse> registrar(@Valid @RequestBody RegistrarPagoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
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
		UUID empresaId = tenant.empresaIdRequerida();
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
		UUID empresaId = tenant.empresaIdRequerida();
		return new SaldoResponse(conceptoId, consultarPagos.saldoNeto(empresaId, conceptoId));
	}

	@GetMapping("/deposito")
	com.costumi.backend.pagos.aplicacion.EstadoDeposito deposito(@RequestParam UUID conceptoId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return consultarPagos.estadoDeposito(empresaId, conceptoId);
	}

	@GetMapping("/comprobante")
	ComprobanteResponse comprobante(@RequestParam UUID conceptoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return ComprobanteResponse.desde(consultarPagos.comprobante(empresaId, conceptoId),
				multas.totalMultaDeRenta(empresaId, conceptoId));
	}

	/** Comprobante/recibo de pago en PDF (RF-3.4). */
	@GetMapping(value = "/comprobante.pdf", produces = "application/pdf")
	ResponseEntity<byte[]> comprobantePdf(@RequestParam UUID conceptoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		ComprobanteResponse c = ComprobanteResponse.desde(consultarPagos.comprobante(empresaId, conceptoId),
				multas.totalMultaDeRenta(empresaId, conceptoId));
		java.util.List<String> lineas = new java.util.ArrayList<>();
		lineas.add("Concepto: " + c.conceptoId());
		lineas.add("Total cobrado: $" + c.totalCobrado());
		lineas.add("Total reembolsado: $" + c.totalReembolsado());
		lineas.add("Saldo neto: $" + c.saldoNeto());
		if (c.multa() != null && c.multa().signum() > 0) {
			lineas.add("Multa: $" + c.multa());
		}
		if (c.impuesto() != null) {
			lineas.add("Base imponible: $" + c.baseImponible());
			lineas.add("Impuesto: $" + c.impuesto());
		}
		lineas.add(" ");
		lineas.add("Detalle de pagos:");
		for (PagoResponse p : c.pagos()) {
			lineas.add(" - " + p.metodo() + " / " + p.tipoPago() + ": $" + p.monto());
		}
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=comprobante.pdf")
				.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
				.body(pdf.documento("Comprobante de pago", lineas));
	}

	record SaldoResponse(UUID conceptoId, java.math.BigDecimal saldoNeto) {
	}
}
