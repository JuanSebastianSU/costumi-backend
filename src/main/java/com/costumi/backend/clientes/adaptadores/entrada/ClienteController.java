package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.aplicacion.CambiarEstadoCliente;
import com.costumi.backend.clientes.aplicacion.CambiarListaNegra;
import com.costumi.backend.clientes.aplicacion.CambiarListaNegraComando;
import com.costumi.backend.clientes.aplicacion.ConsultarClientes;
import com.costumi.backend.clientes.aplicacion.ConsultarHistorial;
import com.costumi.backend.clientes.aplicacion.CrearCliente;
import com.costumi.backend.clientes.aplicacion.CrearClienteComando;
import com.costumi.backend.clientes.aplicacion.EditarCliente;
import com.costumi.backend.clientes.aplicacion.EditarClienteComando;
import com.costumi.backend.clientes.aplicacion.RegistrarDeviceToken;
import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.FiltroDeClientes;
import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.compartido.RespuestaPaginada;
import com.costumi.backend.compartido.SolicitudDePagina;
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
	private final EditarCliente editarCliente;
	private final CambiarEstadoCliente cambiarEstadoCliente;
	private final ConsultarClientes consultarClientes;
	private final CambiarListaNegra cambiarListaNegra;
	private final ConsultarHistorial consultarHistorial;
	private final RegistrarDeviceToken registrarDeviceToken;
	private final ContextoDeTenant tenant;

	ClienteController(CrearCliente crearCliente, EditarCliente editarCliente,
			CambiarEstadoCliente cambiarEstadoCliente, ConsultarClientes consultarClientes,
			CambiarListaNegra cambiarListaNegra, ConsultarHistorial consultarHistorial,
			RegistrarDeviceToken registrarDeviceToken, ContextoDeTenant tenant) {
		this.crearCliente = crearCliente;
		this.editarCliente = editarCliente;
		this.cambiarEstadoCliente = cambiarEstadoCliente;
		this.consultarClientes = consultarClientes;
		this.cambiarListaNegra = cambiarListaNegra;
		this.consultarHistorial = consultarHistorial;
		this.registrarDeviceToken = registrarDeviceToken;
		this.tenant = tenant;
	}

	/**
	 * El propio usuario registra el token de SU dispositivo (RF-18.11). No recibe id: sale del token, asi
	 * que nadie registra el dispositivo de otro.
	 *
	 * <p>Existe aparte del endpoint por id porque aquel exige el claim {@code empresa_id} y un rol de
	 * personal: un CLIENTE del marketplace no tiene ninguno de los dos y no podia registrar su telefono.
	 */
	@PutMapping("/me/device-token")
	ResponseEntity<Void> registrarMiDeviceToken(@Valid @RequestBody DeviceTokenRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		registrarDeviceToken.deUsuario(UUID.fromString(jwt.getSubject()), request.deviceToken());
		return ResponseEntity.noContent().build();
	}

	/** Registra el token de dispositivo de un cliente presencial (lo hace el personal). */
	@PutMapping("/{id}/device-token")
	ResponseEntity<ClienteResponse> registrarDeviceToken(@PathVariable UUID id,
			@Valid @RequestBody DeviceTokenRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		Cliente cliente = registrarDeviceToken.ejecutar(empresaId, id, request.deviceToken());
		return ResponseEntity.ok(ClienteResponse.desde(cliente));
	}

	@PostMapping
	ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		Cliente cliente = crearCliente.ejecutar(new CrearClienteComando(empresaId, request.nombre(),
				request.telefono(), request.email(), request.documento(), request.direccion()));
		URI location = uriBuilder.path("/api/v1/clientes/{id}").buildAndExpand(cliente.id()).toUri();
		return ResponseEntity.created(location).body(ClienteResponse.desde(cliente));
	}

	/** Edita los datos de contacto/identidad de una ficha (RF-7). DUENO/ENCARGADO/MOSTRADOR/ATENCION. */
	@PutMapping("/{id}")
	ClienteResponse editar(@PathVariable UUID id, @Valid @RequestBody EditarClienteRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		Cliente cliente = editarCliente.ejecutar(new EditarClienteComando(empresaId, id, request.nombre(),
				request.telefono(), request.documento(), request.direccion()));
		return ClienteResponse.desde(cliente);
	}

	/** Archiva una ficha: la retira de la lista activa y de nuevas rentas/ventas del personal, sin borrarla. */
	@PostMapping("/{id}/archivar")
	ClienteResponse archivar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return ClienteResponse.desde(cambiarEstadoCliente.archivar(empresaId, id));
	}

	/** Reactiva una ficha archivada. */
	@PostMapping("/{id}/activar")
	ClienteResponse activar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return ClienteResponse.desde(cambiarEstadoCliente.activar(empresaId, id));
	}

	@GetMapping
	RespuestaPaginada<ClienteResponse> listar(@RequestParam(required = false) String buscar,
			@RequestParam(required = false, defaultValue = "false") boolean conPendientes,
			@RequestParam(required = false) String filtro,
			@RequestParam(required = false, defaultValue = "false") boolean incluirArchivados,
			@RequestParam(required = false) Integer pagina, @RequestParam(required = false) Integer tamano,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaIdClaim = jwt.getClaimAsString("empresa_id");
		if (empresaIdClaim == null) {
			return new RespuestaPaginada<>(List.of(), 0, 0, 0, 0);
		}
		UUID empresaId = UUID.fromString(empresaIdClaim);
		// RF-11.5/11.6: filtra la página por categoría de pendiente (PENDIENTES/VENCIDAS/MULTAS/SALDOS).
		// 'filtro' manda; 'conPendientes=true' se mantiene por compatibilidad y equivale a PENDIENTES.
		FiltroDeClientes categoria = resolverFiltro(filtro, conPendientes);
		List<UUID> idsFiltro = categoria == null ? null
				: consultarHistorial.clientesPorFiltro(empresaId, categoria);
		com.costumi.backend.compartido.Pagina<com.costumi.backend.clientes.dominio.Cliente> pagados =
				consultarClientes.listar(empresaId, buscar, idsFiltro, incluirArchivados,
						SolicitudDePagina.de(pagina, tamano));
		List<UUID> idsPagina = pagados.contenido().stream()
				.map(com.costumi.backend.clientes.dominio.Cliente::id).toList();
		var carga = consultarHistorial.cargaDeClientes(empresaId, idsPagina);
		return RespuestaPaginada.desde(pagados, c -> ClienteResponse.desde(c, carga.get(c.id())));
	}

	/** Traduce el parámetro a la categoría de filtro; ignora un valor inválido (equivale a sin filtro). */
	private static FiltroDeClientes resolverFiltro(String filtro, boolean conPendientes) {
		if (filtro != null && !filtro.isBlank()) {
			try {
				return FiltroDeClientes.valueOf(filtro.trim().toUpperCase(java.util.Locale.ROOT));
			}
			catch (IllegalArgumentException e) {
				return null; // valor desconocido: no se aplica filtro.
			}
		}
		return conPendientes ? FiltroDeClientes.PENDIENTES : null;
	}

	@GetMapping("/{id}/historial")
	List<HistorialItem> historial(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return consultarHistorial.historialDeCliente(empresaId, id);
	}

	/** Estado de cuenta del cliente (RF-7/11.5): desglose por renta de cuánto debe y por qué. */
	@GetMapping("/{id}/estado-cuenta")
	EstadoDeCuentaResponse estadoCuenta(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return EstadoDeCuentaResponse.desde(id, consultarHistorial.estadoDeCuenta(empresaId, id));
	}

	/** "Mis Pedidos" del CLIENTE del marketplace: su historial en todas las tiendas (RF-14.4/18.9). */
	@GetMapping("/me/historial")
	List<HistorialItem> miHistorial(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return consultarHistorial.historialDeUsuario(usuarioId);
	}

	/**
	 * Lo que el propio cliente debe: multas y saldos, en TODAS las tiendas (RF-7/11.5). El estado de
	 * cuenta que ya existía es por empresa y lo mira la tienda; el cliente no tenía forma de ver sus
	 * multas ni de saber por qué se las cobraron.
	 *
	 * <p>Se resuelve por el usuario del token (sus fichas), nunca por un id del request.
	 */
	@GetMapping("/me/deudas")
	List<MiDeudaResponse> misDeudas(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return consultarHistorial.misDeudas(usuarioId).stream().map(MiDeudaResponse::desde).toList();
	}

	@PostMapping("/{id}/lista-negra")
	ClienteResponse cambiarListaNegra(@PathVariable UUID id, @RequestBody CambiarListaNegraRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		Cliente cliente = cambiarListaNegra.ejecutar(
				new CambiarListaNegraComando(empresaId, id, request.enListaNegra()));
		return ClienteResponse.desde(cliente);
	}
}
