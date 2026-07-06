package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarrito;
import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarritoComando;
import com.costumi.backend.pedidos.aplicacion.ConsultarCarrito;
import com.costumi.backend.pedidos.aplicacion.HacerCheckout;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
	private final HacerCheckout hacerCheckout;

	CarritoController(AgregarItemAlCarrito agregarItemAlCarrito, ConsultarCarrito consultarCarrito,
			HacerCheckout hacerCheckout) {
		this.agregarItemAlCarrito = agregarItemAlCarrito;
		this.consultarCarrito = consultarCarrito;
		this.hacerCheckout = hacerCheckout;
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

	@PostMapping("/checkout")
	CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID empleadoId = UUID.fromString(jwt.getSubject());
		UUID ventaId = hacerCheckout.ejecutar(empresaId, request.sucursalId(), request.clienteId(), empleadoId);
		return new CheckoutResponse(ventaId);
	}

	/** Checkout del carrito de VENTA (segmentado por sucursal × cliente). */
	record CheckoutRequest(@NotNull UUID sucursalId, @NotNull UUID clienteId) {
	}

	record CheckoutResponse(UUID ventaId) {
	}
}
