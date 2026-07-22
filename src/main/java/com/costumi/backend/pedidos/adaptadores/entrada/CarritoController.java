package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.disfraces.ResolucionDeDisfraces;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarrito;
import com.costumi.backend.pedidos.aplicacion.AgregarItemAlCarritoComando;
import com.costumi.backend.pedidos.aplicacion.CarritoValorizado;
import com.costumi.backend.pedidos.aplicacion.ConsultarCarrito;
import com.costumi.backend.pedidos.aplicacion.ConsultarMisCarritos;
import com.costumi.backend.pedidos.aplicacion.HacerCheckout;
import com.costumi.backend.pedidos.aplicacion.HacerCheckoutDeRenta;
import com.costumi.backend.pedidos.aplicacion.QuitarItemDelCarrito;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.SeleccionDeSlot;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 *       en el request) y el {@code cliente} es su propia ficha en esa tienda — resuelta/creada a
 *       partir de su usuario (RF-14.4). Un cliente nunca opera el carrito de otro.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/carritos")
class CarritoController {

	private final AgregarItemAlCarrito agregarItemAlCarrito;
	private final QuitarItemDelCarrito quitarItemDelCarrito;
	private final ConsultarCarrito consultarCarrito;
	private final ConsultarMisCarritos consultarMisCarritos;
	private final HacerCheckout hacerCheckout;
	private final HacerCheckoutDeRenta hacerCheckoutDeRenta;
	private final ResolucionDeClientes resolucionDeClientes;
	private final ConsultaDeInventario inventario;
	private final ResolucionDeDisfraces disfraces;

	CarritoController(AgregarItemAlCarrito agregarItemAlCarrito, QuitarItemDelCarrito quitarItemDelCarrito,
			ConsultarCarrito consultarCarrito, ConsultarMisCarritos consultarMisCarritos,
			HacerCheckout hacerCheckout, HacerCheckoutDeRenta hacerCheckoutDeRenta,
			ResolucionDeClientes resolucionDeClientes, ConsultaDeInventario inventario,
			ResolucionDeDisfraces disfraces) {
		this.agregarItemAlCarrito = agregarItemAlCarrito;
		this.quitarItemDelCarrito = quitarItemDelCarrito;
		this.consultarCarrito = consultarCarrito;
		this.consultarMisCarritos = consultarMisCarritos;
		this.hacerCheckout = hacerCheckout;
		this.hacerCheckoutDeRenta = hacerCheckoutDeRenta;
		this.resolucionDeClientes = resolucionDeClientes;
		this.inventario = inventario;
		this.disfraces = disfraces;
	}

	@PostMapping("/items")
	CarritoResponse agregarItem(@Valid @RequestBody AgregarItemRequest request, @AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, request.empresaId(), request.clienteId());
		validarUnoUOtro(request);
		List<SeleccionDeSlot> selecciones = (request.selecciones() == null ? List.<AgregarItemRequest.SeleccionSlotDto>of()
				: request.selecciones()).stream()
				.map(s -> new SeleccionDeSlot(s.orden(), s.prendaId()))
				.toList();
		Carrito carrito = agregarItemAlCarrito.ejecutar(new AgregarItemAlCarritoComando(
				actor.empresaId(), request.sucursalId(), actor.clienteId(), request.tipo(),
				request.prendaId(), request.disfrazId(), selecciones, request.cantidad(),
				request.fechaRetiro(), request.fechaDevolucion()));
		return responder(actor.empresaId(), carrito);
	}

	/**
	 * Quita una línea del carrito pendiente (RF-16). El cliente debe poder deshacer lo que agregó: sin
	 * esto, un artículo que ya no se puede valorizar dejaría el carrito bloqueado.
	 */
	@DeleteMapping("/items/{lineaId}")
	CarritoResponse quitarItem(@PathVariable UUID lineaId, @RequestParam UUID sucursalId,
			@RequestParam(required = false) UUID empresaId, @RequestParam(required = false) UUID clienteId,
			@RequestParam TipoPedido tipo, @AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, empresaId, clienteId);
		Carrito carrito = quitarItemDelCarrito.ejecutar(actor.empresaId(), sucursalId, actor.clienteId(), tipo,
				lineaId);
		return responder(actor.empresaId(), carrito);
	}

	/**
	 * Los carritos que el cliente dejó abiertos, en cualquier tienda (RF-16.2/16.3). Sirve para volver a
	 * uno: {@link #pendiente} exige saber de antemano empresa, sucursal y tipo, y el cliente no tenía
	 * dónde verlos — al cambiar de tienda había que agregar un artículo otra vez para reencontrarlo.
	 *
	 * <p>Se resuelve por el <b>usuario del token</b> (sus fichas de cliente), nunca por un id del request.
	 */
	@GetMapping("/mios")
	List<CarritoAbiertoResponse> mios(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return consultarMisCarritos.deUsuario(usuarioId).stream().map(CarritoAbiertoResponse::desde).toList();
	}

	@GetMapping
	CarritoResponse pendiente(@RequestParam UUID sucursalId, @RequestParam(required = false) UUID empresaId,
			@RequestParam(required = false) UUID clienteId, @RequestParam TipoPedido tipo,
			@AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, empresaId, clienteId);
		CarritoValorizado carrito = consultarCarrito.pendiente(actor.empresaId(), sucursalId, actor.clienteId(), tipo);
		return responder(actor.empresaId(), carrito);
	}

	/** Un ítem es una prenda O un disfraz, exactamente uno (RF-16). */
	private static void validarUnoUOtro(AgregarItemRequest request) {
		boolean hayPrenda = request.prendaId() != null;
		boolean hayDisfraz = request.disfrazId() != null;
		if (hayPrenda == hayDisfraz) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Envía una prenda o un disfraz (exactamente uno)");
		}
	}

	/** Arma la respuesta con nombre+foto de prendas y disfraces del carrito (para pintar QUÉ se agregó). */
	private CarritoResponse responder(UUID empresaId, Carrito carrito) {
		List<UUID> prendaIds = carrito.lineas().stream().filter(l -> l.esPrenda()).map(l -> l.prendaId()).toList();
		List<UUID> disfrazIds = carrito.lineas().stream().filter(l -> l.esDisfraz()).map(l -> l.disfrazId()).toList();
		return CarritoResponse.desde(carrito, inventario.resumenDePrendas(empresaId, prendaIds),
				disfraces.resumenDeDisfraces(empresaId, disfrazIds));
	}

	private CarritoResponse responder(UUID empresaId, CarritoValorizado carrito) {
		List<UUID> prendaIds = carrito.lineas().stream().filter(l -> l.prendaId() != null).map(l -> l.prendaId())
				.toList();
		List<UUID> disfrazIds = carrito.lineas().stream().filter(l -> l.disfrazId() != null).map(l -> l.disfrazId())
				.toList();
		return CarritoResponse.desde(carrito, inventario.resumenDePrendas(empresaId, prendaIds),
				disfraces.resumenDeDisfraces(empresaId, disfrazIds));
	}

	@PostMapping("/checkout")
	CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal Jwt jwt) {
		Actor actor = resolver(jwt, request.empresaId(), request.clienteId());
		UUID actorId = UUID.fromString(jwt.getSubject()); // quién confirma (RF-1.4)
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
	 * - CLIENTE (sin {@code empresa_id}): empresa del request; cliente = su ficha en esa empresa
	 *   (resuelta/creada a partir de su usuario del token).
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
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		UUID clienteId = resolucionDeClientes.fichaDeUsuario(empresaIdReq, usuarioId, jwt.getClaimAsString("email"));
		return new Actor(empresaIdReq, clienteId);
	}

	private record Actor(UUID empresaId, UUID clienteId) {
	}

	/**
	 * Checkout del carrito. {@code empresaId}/{@code clienteId} se resuelven por rol (ver {@code resolver}):
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
