package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.aplicacion.ConsultarGruposDeStock;
import com.costumi.backend.inventario.aplicacion.CrearGrupoDeStock;
import com.costumi.backend.inventario.aplicacion.CrearGrupoDeStockComando;
import com.costumi.backend.inventario.aplicacion.MoverUnidades;
import com.costumi.backend.inventario.aplicacion.MoverUnidadesComando;
import com.costumi.backend.inventario.aplicacion.SeleccionVariante;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Grupos de stock (variantes) de una Prenda y sus movimientos de estado (RF-2.2, RF-2.11). */
@RestController
class GrupoDeStockController {

	private final CrearGrupoDeStock crearGrupoDeStock;
	private final ConsultarGruposDeStock consultarGruposDeStock;
	private final MoverUnidades moverUnidades;

	GrupoDeStockController(CrearGrupoDeStock crearGrupoDeStock, ConsultarGruposDeStock consultarGruposDeStock,
			MoverUnidades moverUnidades) {
		this.crearGrupoDeStock = crearGrupoDeStock;
		this.consultarGruposDeStock = consultarGruposDeStock;
		this.moverUnidades = moverUnidades;
	}

	@PostMapping("/api/v1/prendas/{prendaId}/grupos-stock")
	ResponseEntity<GrupoDeStockResponse> crear(@PathVariable UUID prendaId,
			@Valid @RequestBody CrearGrupoDeStockRequest request, @AuthenticationPrincipal Jwt jwt,
			UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		List<SeleccionVariante> combinacion = request.combinacion().stream()
				.map(s -> new SeleccionVariante(s.tipoEtiquetaId(), s.valorEtiquetaId()))
				.toList();
		GrupoDeStock grupo = crearGrupoDeStock.ejecutar(new CrearGrupoDeStockComando(
				empresaId, prendaId, combinacion, request.cantidadInicial()));
		URI location = uriBuilder.path("/api/v1/grupos-stock/{id}").buildAndExpand(grupo.id()).toUri();
		return ResponseEntity.created(location).body(GrupoDeStockResponse.desde(grupo));
	}

	@GetMapping("/api/v1/prendas/{prendaId}/grupos-stock")
	List<GrupoDeStockResponse> listar(@PathVariable UUID prendaId, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarGruposDeStock.dePrenda(UUID.fromString(empresaId), prendaId).stream()
				.map(GrupoDeStockResponse::desde).toList();
	}

	@PostMapping("/api/v1/grupos-stock/{grupoId}/mover")
	GrupoDeStockResponse mover(@PathVariable UUID grupoId, @Valid @RequestBody MoverUnidadesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		GrupoDeStock grupo = moverUnidades.ejecutar(new MoverUnidadesComando(
				empresaId, grupoId, request.desde(), request.hacia(), request.cantidad()));
		return GrupoDeStockResponse.desde(grupo);
	}
}
