package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.aplicacion.CambiarListaNegra;
import com.costumi.backend.clientes.aplicacion.CambiarListaNegraComando;
import com.costumi.backend.clientes.aplicacion.ConsultarClientes;
import com.costumi.backend.clientes.aplicacion.ConsultarHistorial;
import com.costumi.backend.clientes.aplicacion.CrearCliente;
import com.costumi.backend.clientes.aplicacion.CrearClienteComando;
import com.costumi.backend.clientes.aplicacion.RegistrarDeviceToken;
import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.HistorialItem;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Clientes de una Empresa, acotados al tenant del token (RF-7). */
@RestController
@RequestMapping("/api/v1/clientes")
class ClienteController {

	private final CrearCliente crearCliente;
	private final ConsultarClientes consultarClientes;
	private final CambiarListaNegra cambiarListaNegra;
	private final ConsultarHistorial consultarHistorial;
	private final RegistrarDeviceToken registrarDeviceToken;

	ClienteController(CrearCliente crearCliente, ConsultarClientes consultarClientes,
			CambiarListaNegra cambiarListaNegra, ConsultarHistorial consultarHistorial,
			RegistrarDeviceToken registrarDeviceToken) {
		this.crearCliente = crearCliente;
		this.consultarClientes = consultarClientes;
		this.cambiarListaNegra = cambiarListaNegra;
		this.consultarHistorial = consultarHistorial;
		this.registrarDeviceToken = registrarDeviceToken;
	}

	/** Registra el token de dispositivo del cliente para push FCM (RF-18.11). */
	@PutMapping("/{id}/device-token")
	ResponseEntity<ClienteResponse> registrarDeviceToken(@PathVariable UUID id,
			@Valid @RequestBody DeviceTokenRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Cliente cliente = registrarDeviceToken.ejecutar(empresaId, id, request.deviceToken());
		return ResponseEntity.ok(ClienteResponse.desde(cliente));
	}

	@PostMapping
	ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Cliente cliente = crearCliente.ejecutar(new CrearClienteComando(empresaId, request.nombre(),
				request.telefono(), request.email(), request.documento(), request.direccion()));
		URI location = uriBuilder.path("/api/v1/clientes/{id}").buildAndExpand(cliente.id()).toUri();
		return ResponseEntity.created(location).body(ClienteResponse.desde(cliente));
	}

	@GetMapping
	List<ClienteResponse> listar(@RequestParam(required = false) String buscar,
			@RequestParam(required = false, defaultValue = "false") boolean conPendientes,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaIdClaim = jwt.getClaimAsString("empresa_id");
		if (empresaIdClaim == null) {
			return List.of();
		}
		UUID empresaId = UUID.fromString(empresaIdClaim);
		var clientes = consultarClientes.buscar(empresaId, buscar);
		if (conPendientes) {
			// RF-11.5/11.6: solo los clientes con rentas activas pendientes de devolver.
			var pendientes = new java.util.HashSet<>(consultarHistorial.clientesConPendientes(empresaId));
			clientes = clientes.stream().filter(c -> pendientes.contains(c.id())).toList();
		}
		return clientes.stream().map(ClienteResponse::desde).toList();
	}

	@GetMapping("/{id}/historial")
	List<HistorialItem> historial(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarHistorial.historialDeCliente(empresaId, id);
	}

	/** "Mis Pedidos" del CLIENTE del marketplace: su historial en todas las tiendas (RF-14.4/18.9). */
	@GetMapping("/me/historial")
	List<HistorialItem> miHistorial(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return consultarHistorial.historialDeUsuario(usuarioId);
	}

	@PostMapping("/{id}/lista-negra")
	ClienteResponse cambiarListaNegra(@PathVariable UUID id, @RequestBody CambiarListaNegraRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Cliente cliente = cambiarListaNegra.ejecutar(
				new CambiarListaNegraComando(empresaId, id, request.enListaNegra()));
		return ClienteResponse.desde(cliente);
	}
}
