package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.disfraces.aplicacion.AsignarFotoDeDisfraz;
import com.costumi.backend.disfraces.aplicacion.CambiarEstadoDisfraz;
import com.costumi.backend.disfraces.aplicacion.ConsultarDisfraces;
import com.costumi.backend.disfraces.aplicacion.ConsultarDisponibilidadDeDisfraz;
import com.costumi.backend.disfraces.aplicacion.CrearDisfraz;
import com.costumi.backend.disfraces.aplicacion.CrearDisfrazComando;
import com.costumi.backend.disfraces.aplicacion.EditarDisfraz;
import com.costumi.backend.disfraces.aplicacion.EditarDisfrazComando;
import com.costumi.backend.disfraces.aplicacion.PoolComando;
import com.costumi.backend.disfraces.aplicacion.RentarDisfraz;
import com.costumi.backend.disfraces.aplicacion.RentarVariosDisfracesComando;
import com.costumi.backend.disfraces.aplicacion.VenderVariosDisfracesComando;
import com.costumi.backend.disfraces.aplicacion.RentarDisfrazComando;
import com.costumi.backend.disfraces.aplicacion.SlotComando;
import com.costumi.backend.disfraces.aplicacion.VenderDisfraz;
import com.costumi.backend.disfraces.aplicacion.VenderDisfrazComando;
import com.costumi.backend.disfraces.dominio.Disfraz;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Disfraces (Capa 3, RF-2.3/2.4): alta con slots y disponibilidad derivada, acotado al tenant. */
@RestController
@RequestMapping("/api/v1/disfraces")
class DisfrazController {

	private final CrearDisfraz crearDisfraz;
	private final EditarDisfraz editarDisfraz;
	private final CambiarEstadoDisfraz cambiarEstadoDisfraz;
	private final ConsultarDisfraces consultarDisfraces;
	private final ConsultarDisponibilidadDeDisfraz consultarDisponibilidad;
	private final RentarDisfraz rentarDisfraz;
	private final VenderDisfraz venderDisfraz;
	private final com.costumi.backend.disfraces.aplicacion.RentarVariosDisfraces rentarVariosDisfraces;
	private final com.costumi.backend.disfraces.aplicacion.VenderVariosDisfraces venderVariosDisfraces;
	private final AsignarFotoDeDisfraz asignarFotoDeDisfraz;
	private final ContextoDeTenant tenant;
	private final ResolucionDeClientes resolucionDeClientes;
	private final com.costumi.backend.disfraces.aplicacion.GestionCategoriasDeDisfraz categorias;

	DisfrazController(CrearDisfraz crearDisfraz, EditarDisfraz editarDisfraz,
			CambiarEstadoDisfraz cambiarEstadoDisfraz, ConsultarDisfraces consultarDisfraces,
			ConsultarDisponibilidadDeDisfraz consultarDisponibilidad, RentarDisfraz rentarDisfraz,
			VenderDisfraz venderDisfraz,
			com.costumi.backend.disfraces.aplicacion.RentarVariosDisfraces rentarVariosDisfraces,
			com.costumi.backend.disfraces.aplicacion.VenderVariosDisfraces venderVariosDisfraces,
			AsignarFotoDeDisfraz asignarFotoDeDisfraz, ContextoDeTenant tenant,
			ResolucionDeClientes resolucionDeClientes,
			com.costumi.backend.disfraces.aplicacion.GestionCategoriasDeDisfraz categorias) {
		this.crearDisfraz = crearDisfraz;
		this.editarDisfraz = editarDisfraz;
		this.cambiarEstadoDisfraz = cambiarEstadoDisfraz;
		this.consultarDisfraces = consultarDisfraces;
		this.consultarDisponibilidad = consultarDisponibilidad;
		this.rentarDisfraz = rentarDisfraz;
		this.venderDisfraz = venderDisfraz;
		this.rentarVariosDisfraces = rentarVariosDisfraces;
		this.venderVariosDisfraces = venderVariosDisfraces;
		this.asignarFotoDeDisfraz = asignarFotoDeDisfraz;
		this.tenant = tenant;
		this.resolucionDeClientes = resolucionDeClientes;
		this.categorias = categorias;
	}

	/** Sube/actualiza la foto del disfraz (RF-2.9, multipart) — la que sube el dueño para la vitrina. */
	@PostMapping("/{disfrazId}/foto")
	ResponseEntity<DisfrazResponse> subirFoto(@PathVariable UUID disfrazId,
			@RequestParam("archivo") MultipartFile archivo) throws IOException {
		UUID empresaId = tenant.empresaIdRequerida();
		if (archivo == null || archivo.isEmpty()) {
			throw new IllegalArgumentException("El archivo de la foto es obligatorio");
		}
		Disfraz disfraz = asignarFotoDeDisfraz.ejecutar(empresaId, disfrazId, archivo.getBytes());
		return ResponseEntity.ok(resp(empresaId, disfraz));
	}

	/** Respuesta del disfraz con su rango sugerido (renta/venta + multa) calculado en un solo paso. */
	private DisfrazResponse resp(UUID empresaId, Disfraz disfraz) {
		return DisfrazResponse.desde(disfraz, consultarDisfraces.sugeridosDe(empresaId, disfraz));
	}

	@PostMapping
	ResponseEntity<DisfrazResponse> crear(@Valid @RequestBody CrearDisfrazRequest request,
			UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<SlotComando> slots = request.slots().stream().map(DisfrazController::aSlotComando).toList();
		Disfraz disfraz = crearDisfraz.ejecutar(new CrearDisfrazComando(empresaId, request.nombre(),
				request.categoriaId(), slots, request.precioRentaGeneral(), request.precioVentaGeneral(),
				request.tipo()));
		URI location = uriBuilder.path("/api/v1/disfraces/{id}").buildAndExpand(disfraz.id()).toUri();
		return ResponseEntity.created(location).body(resp(empresaId, disfraz));
	}

	/** Edita un disfraz: redefine nombre + slots (RF-2.3). */
	@PutMapping("/{disfrazId}")
	DisfrazResponse editar(@PathVariable UUID disfrazId, @Valid @RequestBody CrearDisfrazRequest request) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<SlotComando> slots = request.slots().stream().map(DisfrazController::aSlotComando).toList();
		Disfraz disfraz = editarDisfraz.ejecutar(new EditarDisfrazComando(empresaId, disfrazId, request.nombre(),
				request.categoriaId(), slots, request.precioRentaGeneral(), request.precioVentaGeneral(),
				request.tipo()));
		return resp(empresaId, disfraz);
	}

	/** Archiva un disfraz: lo retira de la vitrina y del alta de rentas, sin borrarlo. */
	@PostMapping("/{disfrazId}/archivar")
	DisfrazResponse archivar(@PathVariable UUID disfrazId) {
		UUID empresaId = tenant.empresaIdRequerida();
		return resp(empresaId, cambiarEstadoDisfraz.archivar(empresaId, disfrazId));
	}

	/** Reactiva un disfraz archivado. */
	@PostMapping("/{disfrazId}/activar")
	DisfrazResponse activar(@PathVariable UUID disfrazId) {
		UUID empresaId = tenant.empresaIdRequerida();
		return resp(empresaId, cambiarEstadoDisfraz.activar(empresaId, disfrazId));
	}

	/**
	 * Página de disfraces del tenant. {@code buscar} filtra por nombre y {@code categoriaId} por categoría
	 * (RF-2.3). Se pagina en la BD: el cálculo de precios sugeridos solo se hace para la página que se ve.
	 */
	@GetMapping
	com.costumi.backend.compartido.RespuestaPaginada<DisfrazResponse> listar(
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Integer pagina,
			@RequestParam(required = false) Integer tamano) {
		return tenant.empresaId()
				.map(empresaId -> {
					com.costumi.backend.compartido.Pagina<Disfraz> pagados = consultarDisfraces.deEmpresa(
							empresaId, buscar, categoriaId, com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano));
					List<Disfraz> disfraces = pagados.contenido();
					// Un solo cálculo de sugeridos para la página (catálogo cargado una vez): sin N+1.
					Map<UUID, ConsultarDisfraces.Sugeridos> sugeridos = consultarDisfraces.sugeridosDe(empresaId, disfraces);
					// Nombres de categoría en UNA consulta para toda la página (sin N+1).
					Map<UUID, String> nombres = categorias.deEmpresa(empresaId).stream()
							.collect(java.util.stream.Collectors.toMap(
									com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz::id,
									com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz::nombre));
					return com.costumi.backend.compartido.RespuestaPaginada.desde(pagados,
							d -> DisfrazResponse.desde(d, sugeridos.get(d.id()), nombres.get(d.categoriaId())));
				})
				.orElseGet(() -> new com.costumi.backend.compartido.RespuestaPaginada<>(List.of(), 0, 0, 0, 0));
	}

	@GetMapping("/{disfrazId}/disponibilidad")
	DisponibilidadResponse disponibilidad(@PathVariable UUID disfrazId,
			@RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresaDe(jwt, empresaId);
		boolean disponible = consultarDisponibilidad.estaDisponible(empresa, disfrazId);
		return new DisponibilidadResponse(disfrazId, disponible);
	}

	/** Rentar un disfraz (RF-2.3/3.1): lo resuelve a sus prendas y crea la renta. Personal o CLIENTE. */
	@PostMapping("/{disfrazId}/rentar")
	RentarDisfrazResponse rentar(@PathVariable UUID disfrazId, @Valid @RequestBody RentarDisfrazRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = empresaDe(jwt, request.empresaId());
		UUID clienteId = clienteDe(jwt, empresaId, request.clienteId());
		UUID actorId = UUID.fromString(jwt.getSubject()); // quién confirma la renta (RF-1.4)
		List<RentarDisfrazComando.SeleccionDeSlot> selecciones = (request.selecciones() == null
				? List.<RentarDisfrazRequest.SeleccionSlotDto>of() : request.selecciones())
				.stream()
				.map(s -> new RentarDisfrazComando.SeleccionDeSlot(s.orden(), s.prendaId()))
				.toList();
		int cantidad = (request.cantidad() == null || request.cantidad() < 1) ? 1 : request.cantidad();
		UUID rentaId = rentarDisfraz.ejecutar(new RentarDisfrazComando(empresaId, disfrazId, request.sucursalId(),
				clienteId, request.fechaRetiro(), request.fechaDevolucion(), cantidad, selecciones, actorId));
		return new RentarDisfrazResponse(rentaId);
	}

	/** Vender un disfraz: lo resuelve a sus prendas y crea la venta con el precio de venta de cada una. */
	@PostMapping("/{disfrazId}/vender")
	VenderDisfrazResponse vender(@PathVariable UUID disfrazId, @Valid @RequestBody VenderDisfrazRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = empresaDe(jwt, request.empresaId());
		UUID clienteId = clienteDe(jwt, empresaId, request.clienteId());
		UUID actorId = UUID.fromString(jwt.getSubject());
		List<VenderDisfrazComando.SeleccionDeSlot> selecciones = (request.selecciones() == null
				? List.<VenderDisfrazRequest.SeleccionSlotDto>of() : request.selecciones())
				.stream()
				.map(s -> new VenderDisfrazComando.SeleccionDeSlot(s.orden(), s.prendaId()))
				.toList();
		int cantidad = (request.cantidad() == null || request.cantidad() < 1) ? 1 : request.cantidad();
		UUID ventaId = venderDisfraz.ejecutar(new VenderDisfrazComando(empresaId, disfrazId, request.sucursalId(),
				clienteId, cantidad, selecciones, actorId));
		return new VenderDisfrazResponse(ventaId);
	}

	/** Rentar VARIOS disfraces distintos al mismo cliente en una sola renta (RF-3.1). Personal o CLIENTE. */
	@PostMapping("/rentar-varios")
	RentarDisfrazResponse rentarVarios(@Valid @RequestBody RentarVariosDisfracesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = empresaDe(jwt, request.empresaId());
		UUID clienteId = clienteDe(jwt, empresaId, request.clienteId());
		UUID actorId = UUID.fromString(jwt.getSubject());
		if (vacio(request.items()) && vacio(request.lineas())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido debe tener al menos un artículo");
		}
		List<RentarVariosDisfracesComando.ItemDeDisfraz> items = (request.items() == null ? List
				.<RentarVariosDisfracesRequest.ItemDisfrazDto>of() : request.items()).stream()
				.map(it -> new RentarVariosDisfracesComando.ItemDeDisfraz(it.disfrazId(),
						(it.cantidad() == null || it.cantidad() < 1) ? 1 : it.cantidad(),
						(it.selecciones() == null ? List.<RentarVariosDisfracesRequest.SeleccionSlotDto>of()
								: it.selecciones()).stream()
								.map(s -> new RentarVariosDisfracesComando.SeleccionDeSlot(s.orden(), s.prendaId()))
								.toList()))
				.toList();
		List<RentarVariosDisfracesComando.LineaDePrenda> lineas = (request.lineas() == null ? List
				.<RentarVariosDisfracesRequest.LineaPrendaRentaDto>of() : request.lineas()).stream()
				.map(l -> new RentarVariosDisfracesComando.LineaDePrenda(l.prendaId(),
						(l.cantidad() == null || l.cantidad() < 1) ? 1 : l.cantidad(), l.precioPorDia()))
				.toList();
		UUID rentaId = rentarVariosDisfraces.ejecutar(new RentarVariosDisfracesComando(empresaId, request.sucursalId(),
				clienteId, request.fechaRetiro(), request.fechaDevolucion(), items, lineas, actorId));
		return new RentarDisfrazResponse(rentaId);
	}

	/** Vender VARIOS disfraces distintos al mismo cliente en una sola venta. Personal o CLIENTE. */
	@PostMapping("/vender-varios")
	VenderDisfrazResponse venderVarios(@Valid @RequestBody VenderVariosDisfracesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = empresaDe(jwt, request.empresaId());
		UUID clienteId = clienteDe(jwt, empresaId, request.clienteId());
		UUID actorId = UUID.fromString(jwt.getSubject());
		if (vacio(request.items()) && vacio(request.lineas())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido debe tener al menos un artículo");
		}
		List<VenderVariosDisfracesComando.ItemDeDisfraz> items = (request.items() == null ? List
				.<VenderVariosDisfracesRequest.ItemDisfrazDto>of() : request.items()).stream()
				.map(it -> new VenderVariosDisfracesComando.ItemDeDisfraz(it.disfrazId(),
						(it.cantidad() == null || it.cantidad() < 1) ? 1 : it.cantidad(),
						(it.selecciones() == null ? List.<VenderVariosDisfracesRequest.SeleccionSlotDto>of()
								: it.selecciones()).stream()
								.map(s -> new VenderVariosDisfracesComando.SeleccionDeSlot(s.orden(), s.prendaId()))
								.toList()))
				.toList();
		List<VenderVariosDisfracesComando.LineaDePrenda> lineas = (request.lineas() == null ? List
				.<VenderVariosDisfracesRequest.LineaPrendaVentaDto>of() : request.lineas()).stream()
				.map(l -> new VenderVariosDisfracesComando.LineaDePrenda(l.prendaId(),
						(l.cantidad() == null || l.cantidad() < 1) ? 1 : l.cantidad(), l.precioUnitario()))
				.toList();
		UUID ventaId = venderVariosDisfraces.ejecutar(new VenderVariosDisfracesComando(empresaId, request.sucursalId(),
				clienteId, items, lineas, actorId));
		return new VenderDisfrazResponse(ventaId);
	}

	record VenderDisfrazResponse(UUID ventaId) {
	}

	private static boolean vacio(List<?> lista) {
		return lista == null || lista.isEmpty();
	}

	/** Empresa según el actor: del token si es personal; del request/param si es CLIENTE (la tienda). */
	private UUID empresaDe(Jwt jwt, UUID empresaIdReq) {
		String empresaClaim = jwt.getClaimAsString("empresa_id");
		if (empresaClaim != null) {
			return UUID.fromString(empresaClaim);
		}
		if (empresaIdReq == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa (tienda) es obligatoria");
		}
		return empresaIdReq;
	}

	/** Cliente según el actor: la ficha del request si es personal; la propia ficha si es CLIENTE. */
	private UUID clienteDe(Jwt jwt, UUID empresaId, UUID clienteIdReq) {
		if (jwt.getClaimAsString("empresa_id") != null) {
			if (clienteIdReq == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente es obligatorio");
			}
			return clienteIdReq;
		}
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return resolucionDeClientes.fichaDeUsuario(empresaId, usuarioId, jwt.getClaimAsString("email"));
	}

	record RentarDisfrazResponse(UUID rentaId) {
	}

	private static SlotComando aSlotComando(SlotDto s) {
		PoolComando pool = null;
		if (s.pool() != null) {
			Map<UUID, Set<UUID>> etiquetas = new LinkedHashMap<>();
			s.pool().etiquetasPermitidas().forEach(e ->
					etiquetas.put(e.tipoEtiquetaId(), new LinkedHashSet<>(e.valores())));
			pool = new PoolComando(s.pool().categoriaId(), etiquetas);
		}
		return new SlotComando(s.orden(), s.nombre(), s.ejePrenda(), s.prendaFijaId(), pool, s.prendasOpcion(),
				s.opcional());
	}
}
