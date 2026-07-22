package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.aplicacion.ConsultarReembolsos;
import com.costumi.backend.pagos.aplicacion.DecidirReembolso;
import com.costumi.backend.pagos.aplicacion.DecidirReembolsoComando;
import com.costumi.backend.pagos.aplicacion.SolicitarReembolso;
import com.costumi.backend.pagos.aplicacion.SolicitarReembolsoComando;
import com.costumi.backend.pagos.aplicacion.SolicitarReembolsoDeCliente;
import com.costumi.backend.pagos.aplicacion.SolicitarReembolsoDeClienteComando;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Solicitudes de reembolso (RF-4.5/6.9): el personal registra la solicitud del cliente y la decide. */
@RestController
@RequestMapping("/api/v1/reembolsos")
class ReembolsoController {

	private final SolicitarReembolso solicitarReembolso;
	private final SolicitarReembolsoDeCliente solicitarReembolsoDeCliente;
	private final DecidirReembolso decidirReembolso;
	private final ConsultarReembolsos consultarReembolsos;

	ReembolsoController(SolicitarReembolso solicitarReembolso,
			SolicitarReembolsoDeCliente solicitarReembolsoDeCliente, DecidirReembolso decidirReembolso,
			ConsultarReembolsos consultarReembolsos) {
		this.solicitarReembolso = solicitarReembolso;
		this.solicitarReembolsoDeCliente = solicitarReembolsoDeCliente;
		this.decidirReembolso = decidirReembolso;
		this.consultarReembolsos = consultarReembolsos;
	}

	/** Paso 1: registra la solicitud de reembolso (personal). */
	@PostMapping
	ResponseEntity<SolicitudDeReembolsoResponse> solicitar(@Valid @RequestBody SolicitarReembolsoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		SolicitudDeReembolso solicitud = solicitarReembolso.ejecutar(new SolicitarReembolsoComando(empresaId,
				request.tipoConcepto(), request.conceptoId(), request.solicitanteClienteId(), request.monto(),
				request.motivo()));
		URI location = uriBuilder.path("/api/v1/reembolsos/{id}").buildAndExpand(solicitud.id()).toUri();
		return ResponseEntity.created(location).body(SolicitudDeReembolsoResponse.desde(solicitud));
	}

	/** Paso 1 (self-service): el propio CLIENTE solicita el reembolso de su venta/renta desde su cuenta. */
	@PostMapping("/cliente")
	ResponseEntity<SolicitudDeReembolsoResponse> solicitarComoCliente(
			@Valid @RequestBody SolicitarReembolsoDeClienteRequest request, @AuthenticationPrincipal Jwt jwt,
			UriComponentsBuilder uriBuilder) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		SolicitudDeReembolso solicitud = solicitarReembolsoDeCliente.ejecutar(new SolicitarReembolsoDeClienteComando(
				request.empresaId(), usuarioId, jwt.getClaimAsString("email"), request.tipoConcepto(),
				request.conceptoId(), request.monto(), request.motivo()));
		URI location = uriBuilder.path("/api/v1/reembolsos/{id}").buildAndExpand(solicitud.id()).toUri();
		return ResponseEntity.created(location).body(SolicitudDeReembolsoResponse.desde(solicitud));
	}

	/** Paso 2: aprobar (dispara el reembolso; exige el ítem ya devuelto). */
	@PostMapping("/{id}/aprobar")
	SolicitudDeReembolsoResponse aprobar(@PathVariable UUID id, @Valid @RequestBody DecisionReembolsoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return decidir(id, true, request.motivo(), jwt);
	}

	/** Paso 2: rechazar con motivo (un rol superior puede revertirlo luego). */
	@PostMapping("/{id}/rechazar")
	SolicitudDeReembolsoResponse rechazar(@PathVariable UUID id, @Valid @RequestBody DecisionReembolsoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return decidir(id, false, request.motivo(), jwt);
	}

	private SolicitudDeReembolsoResponse decidir(UUID id, boolean aprobar, String motivo, Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID actorUsuarioId = UUID.fromString(jwt.getSubject());
		String actorRol = jwt.getClaimAsString("rol");
		SolicitudDeReembolso solicitud = decidirReembolso.ejecutar(
				new DecidirReembolsoComando(empresaId, id, aprobar, actorUsuarioId, actorRol, motivo));
		return SolicitudDeReembolsoResponse.desde(solicitud);
	}

	/** Bandeja de solicitudes de la empresa (personal). */
	/** Página de solicitudes, más recientes primero. {@code buscar} filtra por el motivo. */
	@GetMapping
	com.costumi.backend.compartido.RespuestaPaginada<SolicitudDeReembolsoResponse> listar(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String buscar,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer pagina,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer tamano,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return new com.costumi.backend.compartido.RespuestaPaginada<>(List.of(), 0, 0, 0, 0);
		}
		return com.costumi.backend.compartido.RespuestaPaginada.desde(
				consultarReembolsos.deEmpresa(UUID.fromString(empresaId), buscar,
						com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano)),
				SolicitudDeReembolsoResponse::desde);
	}
}
