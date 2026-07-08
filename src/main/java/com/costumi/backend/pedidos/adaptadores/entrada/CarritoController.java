package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarrito;
import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarritoComando;
import com.costumi.backend.pedidos.aplicacion.ConsultarCarrito;
import com.costumi.backend.pedidos.aplicacion.HacerCheckout;
import com.costumi.backend.pedidos.aplicacion.HacerCheckoutDeRenta;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Carrito / pedido pendiente (RF-16). Sirve a dos actores:
 * <ul>
 *   <li><b>Personal de la empresa (modo asistido, RF-16.7):</b> la {@code empresa} sale del token y
 *       el {@code cliente} (ficha) viene en el request.</li>
 *   <li><b>CLIENTE del marketplace (RF-18.5):</b> la {@code empresa} es la tienda que compra (viene
 *       en el request) y el {@code cliente} se fuerza a su propio id del token — un cliente nunca
 *       puede operar el carrito de otro.</li>
 * </ul>
 * La segmentación por (sucursal × cliente × tipo) la hace el dominio.
 */
@RestController
@RequestMapping("/api/v1/carritos")
class CarritoController {

	private final AgregarItemAlCarrito agregarItemAlCarrito;
	private final ConsultarCarrito consultarCarrito;
	private final HacerCheckout hacerCheckout;
	private final HacerCheckoutDeRenta hacerCheckoutDeRenta;

	CarritoController(AgregarItemAlCarrito agregarItemAlCarrito, ConsultarCarrito consultarCarrito,
			HacerCheckout hacerCheckout, HacerCheckoutDeRenta hacerCheckoutDeRenta) {
		this.agregarItemAlCarrito = agregarItemAlCarrito;
		this.consultarCarrito = consultarCarrito;
		this.hacerCheckout = hacerCheckout;
		this.hacerCheckoutDeRenta = hacerCheckoutDeRenta;
	}

	@PostMapping("/items")
	CarritoResponse agregarItem(@Valid @RequestBody AgregarItemRequest request, @AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, request.empresaId(), request.clienteId());
		Carrito carrito = agregarItemAlCarrito.ejecutar(new AgregarItemAlCarritoComando(
				actor.empresaId(), request.sucursalId(), actor.clienteId(), request.tipo(),
				request.prendaId(), request.cantidad(), request.fechaRetiro(), request.fechaDevolucion()));
		return CarritoResponse.desde(carrito);
	}

	@GetMapping
	CarritoResponse pendiente(@RequestParam UUID sucursalId, @RequestParam(required = false) UUID empresaId,
			@RequestParam(required = false) UUID clienteId, @RequestParam TipoPedido tipo,
			@AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, empresaId, clienteId);
		return CarritoResponse.desde(consultarCarrito.pendiente(actor.empresaId(), sucursalId, actor.clienteId(), tipo));
	}

	@PostMapping("/checkout")
	CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, request.empresaId(), request.clienteId());
		// El "actor" que confirma la venta (RF-1.4): el empleado logueado o, si es cliente, él mismo.
		UUID actorId = UUID.fromString(jwt.getSubject());
		UUID ventaId = hacerCheckout.ejecutar(actor.empresaId(), request.sucursalId(), actor.clienteId(), actorId);
		return new CheckoutResponse(ventaId);
	}

	@PostMapping("/checkout-renta")
	CheckoutRentaResponse checkoutRenta(@Valid @RequestBody CheckoutRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, request.empresaId(), request.clienteId());
		List<UUID> rentaIds = hacerCheckoutDeRenta.ejecutar(actor.empresaId(), request.sucursalId(), actor.clienteId());
		return new CheckoutRentaResponse(rentaIds);
	}

	/**
	 * Resuelve (empresa, cliente) según el actor:
	 * - Personal (token con {@code empresa_id}): empresa del token; cliente del request (obligatorio).
	 * - CLIENTE (sin {@code empresa_id}): empresa del request (obligatoria); cliente = su propio id del token.
	 */
	private Actor resolver(Jwt jwt, UUID empresaIdReq, UUID clienteIdReq) {
		String empresaClaim = jwt.getClaimAsString("empresa_id");
		if (empresaClaim != null) {
			if (clienteIdReq == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente es obligatorio");
			}
			return new Actor(UUID.fromString(empresaClaim), clienteIdReq);
		}
		if (empresaIdReq == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa (tienda) es obligatoria");
		}
		return new Actor(empresaIdReq, UUID.fromString(jwt.getSubject()));
	}

	private record Actor(UUID empresaId, UUID clienteId) {
	}

	/**
	 * Checkout del carrito. {@code empresaId}/{@code clienteId} se resuelven por rol (ver {@link #resolver}):
	 * el personal manda {@code clienteId}; el CLIENTE manda {@code empresaId} (la tienda).
	 */
	record CheckoutRequest(@NotNull UUID sucursalId, UUID empresaId, UUID clienteId) {
	}

	record CheckoutResponse(UUID ventaId) {
	}

	/** Resultado del checkout de RENTA: una renta por cada periodo del carrito (RF-18.6). */
	record CheckoutRentaResponse(List<UUID> rentaIds) {
	}
}
