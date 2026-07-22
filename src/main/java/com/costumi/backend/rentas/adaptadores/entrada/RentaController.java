package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.compartido.RespuestaPaginada;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.rentas.aplicacion.ConsultarRentas;
import com.costumi.backend.rentas.aplicacion.CrearRenta;
import com.costumi.backend.rentas.aplicacion.CrearRentaComando;
import com.costumi.backend.rentas.aplicacion.GestionarRenta;
import com.costumi.backend.rentas.aplicacion.LineaDeRentaComando;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaLinea;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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

/** Rentas (RF-3), acotadas al tenant del token. Modo asistido: el empleado indica cliente y prenda. */
@RestController
@RequestMapping("/api/v1/rentas")
class RentaController {

	private final CrearRenta crearRenta;
	private final ConsultarRentas consultarRentas;
	private final GestionarRenta gestionarRenta;
	private final ConsultaDeInventario inventario;
	private final com.costumi.backend.compartido.GeneradorDePdf pdf;

	private final com.costumi.backend.clientes.ResolucionDeClientes clientes;

	RentaController(CrearRenta crearRenta, ConsultarRentas consultarRentas, GestionarRenta gestionarRenta,
			ConsultaDeInventario inventario, com.costumi.backend.compartido.GeneradorDePdf pdf,
			com.costumi.backend.clientes.ResolucionDeClientes clientes) {
		this.crearRenta = crearRenta;
		this.consultarRentas = consultarRentas;
		this.gestionarRenta = gestionarRenta;
		this.inventario = inventario;
		this.pdf = pdf;
		this.clientes = clientes;
	}

	/** Construye la respuesta con líneas enriquecidas (nombre + foto) y el nombre del cliente, para el listado. */
	private RentaResponse resp(UUID empresaId, Renta r) {
		List<UUID> prendaIds = r.lineas().stream().map(RentaLinea::prendaId).toList();
		String clienteNombre = r.clienteId() == null ? null
				: clientes.nombreDeCliente(empresaId, r.clienteId()).orElse(null);
		return RentaResponse.desde(r, inventario.resumenDePrendas(empresaId, prendaIds), clienteNombre);
	}

	/** Contrato de renta en PDF (RF-3.4). */
	@GetMapping(value = "/{id}/contrato.pdf", produces = "application/pdf")
	ResponseEntity<byte[]> contrato(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Renta renta = consultarRentas.buscarPorId(empresaId, id)
				.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
						org.springframework.http.HttpStatus.NOT_FOUND, "Renta no encontrada"));
		RentaResponse r = resp(empresaId, renta);
		java.util.List<String> lineas = new java.util.ArrayList<>();
		lineas.add("Renta: " + r.id());
		lineas.add("Cliente: " + r.clienteId());
		lineas.add("Artículos:");
		for (LineaRentaResponse linea : r.lineas()) {
			String articulo = linea.nombre() != null ? linea.nombre() : "Prenda " + linea.prendaId();
			lineas.add("  - " + articulo + " x" + linea.cantidad() + " ($" + linea.precioPorDia() + "/día)");
		}
		lineas.add("Fecha de retiro: " + r.fechaRetiro());
		lineas.add("Fecha de devolución: " + r.fechaDevolucion());
		lineas.add("Importe: $" + r.importe());
		lineas.add("Depósito: $" + r.deposito());
		lineas.add("Estado: " + r.estado());
		lineas.add(" ");
		lineas.add("El cliente se compromete a devolver la(s) prenda(s) en la fecha indicada y en buen estado. "
				+ "El depósito se reintegra al cierre conforme al estado de la devolución.");
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=contrato-renta.pdf")
				.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
				.body(pdf.documento("Contrato de renta", lineas));
	}

	@PostMapping
	ResponseEntity<RentaResponse> crear(@Valid @RequestBody CrearRentaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID empleadoId = UUID.fromString(jwt.getSubject());
		Renta renta = crearRenta.ejecutar(new CrearRentaComando(empresaId, request.sucursalId(), request.clienteId(),
				lineasDe(request), request.fechaRetiro(), request.fechaDevolucion(), request.deposito(),
				request.claveIdempotencia(), empleadoId));
		URI location = uriBuilder.path("/api/v1/rentas/{id}").buildAndExpand(renta.id()).toUri();
		return ResponseEntity.created(location).body(resp(empresaId, renta));
	}

	/**
	 * Normaliza el request a líneas: usa {@code lineas} si viene; si no, la forma de un solo artículo
	 * ({@code prendaId} + {@code precioPorDia}, cantidad 1). Si no viene ninguna, 400.
	 */
	private static List<LineaDeRentaComando> lineasDe(CrearRentaRequest request) {
		if (request.lineas() != null && !request.lineas().isEmpty()) {
			return request.lineas().stream()
					.map(l -> new LineaDeRentaComando(l.prendaId(), l.cantidad(), l.precioPorDia()))
					.toList();
		}
		if (request.prendaId() != null && request.precioPorDia() != null) {
			return List.of(new LineaDeRentaComando(request.prendaId(), 1, request.precioPorDia()));
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Indica al menos un artículo: 'lineas' o 'prendaId' + 'precioPorDia'");
	}

	@GetMapping
	RespuestaPaginada<RentaResponse> listar(@RequestParam(required = false) UUID clienteId,
			@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Integer pagina, @RequestParam(required = false) Integer tamano,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return new RespuestaPaginada<>(List.of(), 0, 0, 0, 0);
		}
		UUID empresa = UUID.fromString(empresaId);
		return RespuestaPaginada.desde(
				consultarRentas.listar(empresa, clienteId, buscar, SolicitudDePagina.de(pagina, tamano)),
				r -> resp(empresa, r));
	}

	/** Una renta por id, con sus líneas (nombre + foto), para el detalle de cobros/reembolsos. */
	@GetMapping("/{id}")
	RentaResponse porId(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresa(jwt);
		Renta renta = consultarRentas.buscarPorId(empresa, id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renta no encontrada"));
		return resp(empresa, renta);
	}

	@PostMapping("/{id}/entregar")
	RentaResponse entregar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresa(jwt);
		return resp(empresa, gestionarRenta.entregar(empresa, id));
	}

	@PostMapping("/{id}/devolver")
	RentaResponse devolver(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresa(jwt);
		return resp(empresa, gestionarRenta.devolver(empresa, id));
	}

	@PostMapping("/{id}/cerrar")
	RentaResponse cerrar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresa(jwt);
		return resp(empresa, gestionarRenta.cerrar(empresa, id));
	}

	@PostMapping("/{id}/extender")
	RentaResponse extender(@PathVariable UUID id, @RequestBody ExtenderRentaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresa(jwt);
		return resp(empresa, gestionarRenta.extender(empresa, id, request.nuevaFechaDevolucion()));
	}

	record ExtenderRentaRequest(java.time.LocalDate nuevaFechaDevolucion) {
	}

	@PostMapping("/{id}/cancelar")
	RentaResponse cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresa = empresa(jwt);
		return resp(empresa, gestionarRenta.cancelar(empresa, id));
	}

	private static UUID empresa(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("empresa_id"));
	}
}
