package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.aplicacion.CambiarListaNegra;
import com.costumi.backend.clientes.aplicacion.CambiarListaNegraComando;
import com.costumi.backend.clientes.aplicacion.ConsultarClientes;
import com.costumi.backend.clientes.aplicacion.CrearCliente;
import com.costumi.backend.clientes.aplicacion.CrearClienteComando;
import com.costumi.backend.clientes.dominio.Cliente;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	ClienteController(CrearCliente crearCliente, ConsultarClientes consultarClientes,
			CambiarListaNegra cambiarListaNegra) {
		this.crearCliente = crearCliente;
		this.consultarClientes = consultarClientes;
		this.cambiarListaNegra = cambiarListaNegra;
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
	List<ClienteResponse> listar(@RequestParam(required = false) String buscar, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarClientes.buscar(UUID.fromString(empresaId), buscar).stream()
				.map(ClienteResponse::desde).toList();
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
