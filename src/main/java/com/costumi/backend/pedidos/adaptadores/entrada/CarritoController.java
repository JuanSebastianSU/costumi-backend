package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarrito;
import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarritoComando;
import com.costumi.backend.pedidos.aplicacion.ConsultarCarrito;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Carrito / pedido pendiente (RF-16). Modo asistido: el empleado indica el cliente. El tenant
 * (empresa) sale del token; la segmentación por (sucursal × cliente × tipo) la hace el dominio.
 */
@RestController
@RequestMapping("/api/v1/carritos")
class CarritoController {

	private final AgregarItemAlCarrito agregarItemAlCarrito;
	private final ConsultarCarrito consultarCarrito;

	CarritoController(AgregarItemAlCarrito agregarItemAlCarrito, ConsultarCarrito consultarCarrito) {
		this.agregarItemAlCarrito = agregarItemAlCarrito;
		this.consultarCarrito = consultarCarrito;
	}

	@PostMapping("/items")
	CarritoResponse agregarItem(@Valid @RequestBody AgregarItemRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Carrito carrito = agregarItemAlCarrito.ejecutar(new AgregarItemAlCarritoComando(
				empresaId, request.sucursalId(), request.clienteId(), request.tipo(),
				request.prendaId(), request.cantidad()));
		return CarritoResponse.desde(carrito);
	}

	@GetMapping
	CarritoResponse pendiente(@RequestParam UUID sucursalId, @RequestParam UUID clienteId,
			@RequestParam TipoPedido tipo, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return CarritoResponse.desde(consultarCarrito.pendiente(empresaId, sucursalId, clienteId, tipo));
	}
}
