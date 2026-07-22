package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.notificaciones.aplicacion.AvisarStockBajo;
import com.costumi.backend.notificaciones.aplicacion.ConsultarNotificaciones;
import com.costumi.backend.notificaciones.aplicacion.EnviarNotificacion;
import com.costumi.backend.notificaciones.aplicacion.EnviarNotificacionComando;
import com.costumi.backend.notificaciones.aplicacion.RecordarProximas;
import com.costumi.backend.notificaciones.aplicacion.RecordarVencidas;
import com.costumi.backend.notificaciones.dominio.Notificacion;
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

/** Notificaciones (RF-11), acotadas al tenant del token. */
@RestController
@RequestMapping("/api/v1/notificaciones")
class NotificacionController {

	private final EnviarNotificacion enviarNotificacion;
	private final ConsultarNotificaciones consultarNotificaciones;
	private final RecordarVencidas recordarVencidas;
	private final RecordarProximas recordarProximas;
	private final AvisarStockBajo avisarStockBajo;
	private final com.costumi.backend.notificaciones.aplicacion.ConsultarEstadoDeCanales estadoDeCanales;
	private final ContextoDeTenant tenant;

	NotificacionController(EnviarNotificacion enviarNotificacion, ConsultarNotificaciones consultarNotificaciones,
			RecordarVencidas recordarVencidas, RecordarProximas recordarProximas, AvisarStockBajo avisarStockBajo,
			com.costumi.backend.notificaciones.aplicacion.ConsultarEstadoDeCanales estadoDeCanales, ContextoDeTenant tenant) {
		this.estadoDeCanales = estadoDeCanales;
		this.enviarNotificacion = enviarNotificacion;
		this.consultarNotificaciones = consultarNotificaciones;
		this.recordarVencidas = recordarVencidas;
		this.recordarProximas = recordarProximas;
		this.avisarStockBajo = avisarStockBajo;
		this.tenant = tenant;
	}

	@PostMapping
	ResponseEntity<NotificacionResponse> enviar(@Valid @RequestBody EnviarNotificacionRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		Notificacion notificacion = enviarNotificacion.ejecutar(new EnviarNotificacionComando(
				empresaId, request.clienteId(), request.canal(), request.mensaje()));
		URI location = uriBuilder.path("/api/v1/notificaciones/{id}").buildAndExpand(notificacion.id()).toUri();
		return ResponseEntity.created(location).body(NotificacionResponse.desde(notificacion));
	}

	@PostMapping("/recordar-vencidas")
	RecordatorioResponse recordarVencidas(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return new RecordatorioResponse(recordarVencidas.ejecutar(empresaId));
	}

	/** Dispara manualmente el recordatorio ANTICIPADO (rentas que vencen dentro de la ventana). */
	@PostMapping("/recordar-proximas")
	RecordatorioResponse recordarProximas(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return new RecordatorioResponse(recordarProximas.ejecutar(empresaId));
	}

	/** Dispara manualmente el aviso proactivo de stock bajo al dueño (RF-11.2). */
	@PostMapping("/avisar-stock-bajo")
	RecordatorioResponse avisarStockBajo(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return new RecordatorioResponse(avisarStockBajo.ejecutar(empresaId));
	}

	record RecordatorioResponse(int enviadas) {
	}

	/**
	 * Página de notificaciones, más recientes primero. {@code buscar} filtra por el texto del mensaje.
	 * Se pagina siempre: se genera una por cada aviso y devolverlas todas deja de funcionar con el tiempo.
	 */
	@GetMapping
	com.costumi.backend.compartido.RespuestaPaginada<NotificacionResponse> listar(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String buscar,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer pagina,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer tamano,
			@AuthenticationPrincipal Jwt jwt) {
		// Lista: sin empresa devuelve vacio (no 403), igual que el resto de listas de gestion.
		UUID empresaId = tenant.empresaId().orElse(null);
		if (empresaId == null) {
			return new com.costumi.backend.compartido.RespuestaPaginada<>(List.of(), 0, 0, 0, 0);
		}
		return com.costumi.backend.compartido.RespuestaPaginada.desde(
				consultarNotificaciones.deEmpresa(empresaId, buscar,
						com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano)),
				NotificacionResponse::desde);
	}

	/**
	 * Estado de los canales externos. Responde "por que no llego la push" sin mirar logs ni exponer
	 * credenciales: si un canal no esta configurado, el aviso cae al log y queda como ENVIADA igual.
	 */
	@GetMapping("/estado-canales")
	com.costumi.backend.notificaciones.aplicacion.EstadoDeCanales estadoDeCanales() {
		return estadoDeCanales.estado();
	}

	/**
	 * Push de prueba al dispositivo del cliente; devuelve el motivo exacto si no sale. El cliente tiene
	 * que ser de TU tienda: la empresa sale del token, nunca del request.
	 */
	@PostMapping("/probar-push/{clienteId}")
	com.costumi.backend.notificaciones.aplicacion.ResultadoDePrueba probarPush(@PathVariable UUID clienteId,
			@AuthenticationPrincipal Jwt jwt) {
		return estadoDeCanales.probarPush(tenant.empresaIdRequerida(), clienteId);
	}
}
