package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.devoluciones.aplicacion.ConsultarDevoluciones;
import com.costumi.backend.devoluciones.aplicacion.RegistrarDevolucion;
import com.costumi.backend.devoluciones.aplicacion.RegistrarDevolucionComando;
import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.devoluciones.dominio.PiezaRevisada;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Devoluciones de renta (RF-5): checklist + liquidación del depósito, acotado al tenant. */
@RestController
@RequestMapping("/api/v1/devoluciones")
class DevolucionController {

	private final RegistrarDevolucion registrarDevolucion;
	private final ConsultarDevoluciones consultarDevoluciones;

	DevolucionController(RegistrarDevolucion registrarDevolucion, ConsultarDevoluciones consultarDevoluciones) {
		this.registrarDevolucion = registrarDevolucion;
		this.consultarDevoluciones = consultarDevoluciones;
	}

	@PostMapping
	ResponseEntity<DevolucionResponse> registrar(@Valid @RequestBody RegistrarDevolucionRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		List<PiezaRevisada> piezas = (request.piezas() == null) ? List.of()
				: request.piezas().stream()
						.map(p -> PiezaRevisada.de(p.descripcion(), p.llego(), p.estado()))
						.toList();
		Devolucion devolucion = registrarDevolucion.ejecutar(new RegistrarDevolucionComando(empresaId,
				request.rentaId(), request.deposito(), request.cargoPorDanos(), request.cargoPorRetraso(), piezas));
		URI location = uriBuilder.path("/api/v1/devoluciones/{id}").buildAndExpand(devolucion.id()).toUri();
		return ResponseEntity.created(location).body(DevolucionResponse.desde(devolucion));
	}

	@GetMapping
	List<DevolucionResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarDevoluciones.deEmpresa(UUID.fromString(empresaId)).stream()
				.map(DevolucionResponse::desde).toList();
	}
}
