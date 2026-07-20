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
	private final AsignarFotoDeDisfraz asignarFotoDeDisfraz;
	private final ContextoDeTenant tenant;
	private final ResolucionDeClientes resolucionDeClientes;

	DisfrazController(CrearDisfraz crearDisfraz, EditarDisfraz editarDisfraz,
			CambiarEstadoDisfraz cambiarEstadoDisfraz, ConsultarDisfraces consultarDisfraces,
			ConsultarDisponibilidadDeDisfraz consultarDisponibilidad, RentarDisfraz rentarDisfraz,
			VenderDisfraz venderDisfraz, AsignarFotoDeDisfraz asignarFotoDeDisfraz, ContextoDeTenant tenant,
			ResolucionDeClientes resolucionDeClientes) {
		this.crearDisfraz = crearDisfraz;
		this.editarDisfraz = editarDisfraz;
		this.cambiarEstadoDisfraz = cambiarEstadoDisfraz;
		this.consultarDisfraces = consultarDisfraces;
		this.consultarDisponibilidad = consultarDisponibilidad;
		this.rentarDisfraz = rentarDisfraz;
		this.venderDisfraz = venderDisfraz;
		this.asignarFotoDeDisfraz = asignarFotoDeDisfraz;
		this.tenant = tenant;
		this.resolucionDeClientes = resolucionDeClientes;
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

	/** Respuesta del disfraz con sus precios sugeridos (renta y venta, suma de las prendas) calculados. */
	private DisfrazResponse resp(UUID empresaId, Disfraz disfraz) {
		return DisfrazResponse.desde(disfraz, consultarDisfraces.precioRentaSugerido(empresaId, disfraz),
				consultarDisfraces.precioVentaSugerido(empresaId, disfraz));
	}

	@PostMapping
	ResponseEntity<DisfrazResponse> crear(@Valid @RequestBody CrearDisfrazRequest request,
			UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<SlotComando> slots = request.slots().stream().map(DisfrazController::aSlotComando).toList();
		Disfraz disfraz = crearDisfraz.ejecutar(
				new CrearDisfrazComando(empresaId, request.nombre(), slots, request.precioRentaGeneral()));
		URI location = uriBuilder.path("/api/v1/disfraces/{id}").buildAndExpand(disfraz.id()).toUri();
		return ResponseEntity.created(location).body(resp(empresaId, disfraz));
	}

	/** Edita un disfraz: redefine nombre + slots (RF-2.3). */
	@PutMapping("/{disfrazId}")
	DisfrazResponse editar(@PathVariable UUID disfrazId, @Valid @RequestBody CrearDisfrazRequest request) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<SlotComando> slots = request.slots().stream().map(DisfrazController::aSlotComando).toList();
		Disfraz disfraz = editarDisfraz.ejecutar(
				new EditarDisfrazComando(empresaId, disfrazId, request.nombre(), slots, request.precioRentaGeneral()));
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

	@GetMapping
	List<DisfrazResponse> listar() {
		return tenant.empresaId()
				.map(empresaId -> consultarDisfraces.deEmpresa(empresaId).stream()
						.map(d -> resp(empresaId, d)).toList())
				.orElseGet(List::of);
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
		UUID rentaId = rentarDisfraz.ejecutar(new RentarDisfrazComando(empresaId, disfrazId, request.sucursalId(),
				clienteId, request.fechaRetiro(), request.fechaDevolucion(), selecciones, actorId));
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
		UUID ventaId = venderDisfraz.ejecutar(new VenderDisfrazComando(empresaId, disfrazId, request.sucursalId(),
				clienteId, selecciones, actorId));
		return new VenderDisfrazResponse(ventaId);
	}

	record VenderDisfrazResponse(UUID ventaId) {
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
		return new SlotComando(s.orden(), s.nombre(), s.ejePrenda(), s.prendaFijaId(), pool, s.opcional());
	}
}
