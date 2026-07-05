package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.disfraces.aplicacion.ConsultarDisfraces;
import com.costumi.backend.disfraces.aplicacion.ConsultarDisponibilidadDeDisfraz;
import com.costumi.backend.disfraces.aplicacion.CrearDisfraz;
import com.costumi.backend.disfraces.aplicacion.CrearDisfrazComando;
import com.costumi.backend.disfraces.aplicacion.PoolComando;
import com.costumi.backend.disfraces.aplicacion.SlotComando;
import com.costumi.backend.disfraces.dominio.Disfraz;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

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
	private final ConsultarDisfraces consultarDisfraces;
	private final ConsultarDisponibilidadDeDisfraz consultarDisponibilidad;
	private final ContextoDeTenant tenant;

	DisfrazController(CrearDisfraz crearDisfraz, ConsultarDisfraces consultarDisfraces,
			ConsultarDisponibilidadDeDisfraz consultarDisponibilidad, ContextoDeTenant tenant) {
		this.crearDisfraz = crearDisfraz;
		this.consultarDisfraces = consultarDisfraces;
		this.consultarDisponibilidad = consultarDisponibilidad;
		this.tenant = tenant;
	}

	@PostMapping
	ResponseEntity<DisfrazResponse> crear(@Valid @RequestBody CrearDisfrazRequest request,
			UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<SlotComando> slots = request.slots().stream().map(DisfrazController::aSlotComando).toList();
		Disfraz disfraz = crearDisfraz.ejecutar(new CrearDisfrazComando(
				empresaId, request.nombre(), request.modo(), request.prendaFijaId(), slots));
		URI location = uriBuilder.path("/api/v1/disfraces/{id}").buildAndExpand(disfraz.id()).toUri();
		return ResponseEntity.created(location).body(DisfrazResponse.desde(disfraz));
	}

	@GetMapping
	List<DisfrazResponse> listar() {
		return tenant.empresaId()
				.map(empresaId -> consultarDisfraces.deEmpresa(empresaId).stream().map(DisfrazResponse::desde).toList())
				.orElseGet(List::of);
	}

	@GetMapping("/{disfrazId}/disponibilidad")
	DisponibilidadResponse disponibilidad(@PathVariable UUID disfrazId) {
		boolean disponible = consultarDisponibilidad.estaDisponible(tenant.empresaIdRequerida(), disfrazId);
		return new DisponibilidadResponse(disfrazId, disponible);
	}

	private static SlotComando aSlotComando(SlotDto s) {
		PoolComando pool = null;
		if (s.pool() != null) {
			Map<UUID, Set<UUID>> etiquetas = new LinkedHashMap<>();
			s.pool().etiquetasPermitidas().forEach(e ->
					etiquetas.put(e.tipoEtiquetaId(), new LinkedHashSet<>(e.valores())));
			pool = new PoolComando(s.pool().categoriaId(), etiquetas);
		}
		return new SlotComando(s.orden(), s.nombre(), s.ejeTalla(), s.tallaFija(), s.ejePrenda(), s.prendaFijaId(),
				pool, s.opcional());
	}
}
