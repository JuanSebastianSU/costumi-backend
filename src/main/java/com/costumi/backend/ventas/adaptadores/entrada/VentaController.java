package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.ventas.aplicacion.ConsultarVentas;
import com.costumi.backend.ventas.aplicacion.DevolverVenta;
import com.costumi.backend.ventas.aplicacion.RegistrarVenta;
import com.costumi.backend.ventas.aplicacion.RegistrarVentaComando;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.Venta;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ventas / POS (RF-4), acotadas al tenant. La venta va a nombre del empleado del token (RF-4.2). */
@RestController
@RequestMapping("/api/v1/ventas")
class VentaController {

	private final RegistrarVenta registrarVenta;
	private final ConsultarVentas consultarVentas;
	private final DevolverVenta devolverVenta;

	VentaController(RegistrarVenta registrarVenta, ConsultarVentas consultarVentas, DevolverVenta devolverVenta) {
		this.registrarVenta = registrarVenta;
		this.consultarVentas = consultarVentas;
		this.devolverVenta = devolverVenta;
	}

	/**
	 * Devolución de una venta (RF-4.5). Sin cuerpo (o {@code lineas} vacío) devuelve <b>todo lo pendiente</b>;
	 * con {@code lineas} devuelve solo esas unidades (reembolso parcial). Respeta la política del local.
	 */
	@PostMapping("/{id}/devolver")
	VentaResponse devolver(@PathVariable UUID id, @RequestBody(required = false) DevolverVentaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Map<UUID, Integer> cantidades = new LinkedHashMap<>();
		if (request != null && request.lineas() != null) {
			for (DevolverVentaRequest.LineaADevolver linea : request.lineas()) {
				cantidades.merge(linea.prendaId(), linea.cantidad(), Integer::sum);
			}
		}
		return VentaResponse.desde(devolverVenta.devolver(empresaId, id, cantidades));
	}

	@PostMapping
	ResponseEntity<VentaResponse> registrar(@Valid @RequestBody RegistrarVentaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID empleadoId = UUID.fromString(jwt.getSubject());
		List<LineaDeVenta> lineas = request.lineas().stream()
				.map(l -> LineaDeVenta.de(l.prendaId(), l.cantidad(), l.precioUnitario()))
				.toList();
		Venta venta = registrarVenta.ejecutar(new RegistrarVentaComando(empresaId, request.sucursalId(),
				empleadoId, request.clienteId(), request.descuento(), lineas, request.claveIdempotencia()));
		URI location = uriBuilder.path("/api/v1/ventas/{id}").buildAndExpand(venta.id()).toUri();
		return ResponseEntity.created(location).body(VentaResponse.desde(venta));
	}

	@GetMapping
	List<VentaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarVentas.deEmpresa(UUID.fromString(empresaId)).stream().map(VentaResponse::desde).toList();
	}
}
