package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.aplicacion.ConsultarRentas;
import com.costumi.backend.rentas.aplicacion.CrearRenta;
import com.costumi.backend.rentas.aplicacion.CrearRentaComando;
import com.costumi.backend.rentas.aplicacion.GestionarRenta;
import com.costumi.backend.rentas.dominio.Renta;
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
	private final com.costumi.backend.compartido.GeneradorDePdf pdf;

	RentaController(CrearRenta crearRenta, ConsultarRentas consultarRentas, GestionarRenta gestionarRenta,
			com.costumi.backend.compartido.GeneradorDePdf pdf) {
		this.crearRenta = crearRenta;
		this.consultarRentas = consultarRentas;
		this.gestionarRenta = gestionarRenta;
		this.pdf = pdf;
	}

	/** Contrato de renta en PDF (RF-3.4). */
	@GetMapping(value = "/{id}/contrato.pdf", produces = "application/pdf")
	ResponseEntity<byte[]> contrato(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		RentaResponse r = consultarRentas.buscarPorId(empresaId, id).map(RentaResponse::desde)
				.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
						org.springframework.http.HttpStatus.NOT_FOUND, "Renta no encontrada"));
		java.util.List<String> lineas = java.util.List.of(
				"Renta: " + r.id(),
				"Cliente: " + r.clienteId(),
				"Prenda: " + r.prendaId(),
				"Fecha de retiro: " + r.fechaRetiro(),
				"Fecha de devolución: " + r.fechaDevolucion(),
				"Precio por día: $" + r.precioPorDia(),
				"Importe: $" + r.importe(),
				"Depósito: $" + r.deposito(),
				"Estado: " + r.estado(),
				" ",
				"El cliente se compromete a devolver la(s) prenda(s) en la fecha indicada y en buen estado. "
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
		Renta renta = crearRenta.ejecutar(new CrearRentaComando(empresaId, request.sucursalId(), request.clienteId(),
				request.prendaId(), request.fechaRetiro(), request.fechaDevolucion(), request.precioPorDia(),
				request.deposito(), request.claveIdempotencia()));
		URI location = uriBuilder.path("/api/v1/rentas/{id}").buildAndExpand(renta.id()).toUri();
		return ResponseEntity.created(location).body(RentaResponse.desde(renta));
	}

	@GetMapping
	List<RentaResponse> listar(@RequestParam(required = false) UUID clienteId, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarRentas.buscar(UUID.fromString(empresaId), clienteId).stream()
				.map(RentaResponse::desde).toList();
	}

	@PostMapping("/{id}/entregar")
	RentaResponse entregar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return RentaResponse.desde(gestionarRenta.entregar(empresa(jwt), id));
	}

	@PostMapping("/{id}/devolver")
	RentaResponse devolver(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return RentaResponse.desde(gestionarRenta.devolver(empresa(jwt), id));
	}

	@PostMapping("/{id}/cerrar")
	RentaResponse cerrar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return RentaResponse.desde(gestionarRenta.cerrar(empresa(jwt), id));
	}

	@PostMapping("/{id}/extender")
	RentaResponse extender(@PathVariable UUID id, @RequestBody ExtenderRentaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return RentaResponse.desde(gestionarRenta.extender(empresa(jwt), id, request.nuevaFechaDevolucion()));
	}

	record ExtenderRentaRequest(java.time.LocalDate nuevaFechaDevolucion) {
	}

	@PostMapping("/{id}/cancelar")
	RentaResponse cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return RentaResponse.desde(gestionarRenta.cancelar(empresa(jwt), id));
	}

	private static UUID empresa(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("empresa_id"));
	}
}
