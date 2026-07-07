package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.aplicacion.AjustarStock;
import com.costumi.backend.inventario.aplicacion.ConsultarGruposDeStock;
import com.costumi.backend.inventario.aplicacion.ConsultarStockBajo;
import com.costumi.backend.inventario.aplicacion.CrearGrupoDeStock;
import com.costumi.backend.inventario.aplicacion.CrearGrupoDeStockComando;
import com.costumi.backend.inventario.aplicacion.MoverUnidades;
import com.costumi.backend.inventario.aplicacion.MoverUnidadesComando;
import com.costumi.backend.inventario.aplicacion.ReabastecerGrupo;
import com.costumi.backend.inventario.aplicacion.SeleccionVariante;
import com.costumi.backend.inventario.aplicacion.TransferirStock;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Grupos de stock (variantes) de una Prenda, movimientos de estado y reabastecimiento (RF-2.2/2.11/10). */
@RestController
class GrupoDeStockController {

	private final CrearGrupoDeStock crearGrupoDeStock;
	private final ConsultarGruposDeStock consultarGruposDeStock;
	private final MoverUnidades moverUnidades;
	private final ReabastecerGrupo reabastecerGrupo;
	private final ConsultarStockBajo consultarStockBajo;
	private final AjustarStock ajustarStock;
	private final TransferirStock transferirStock;

	GrupoDeStockController(CrearGrupoDeStock crearGrupoDeStock, ConsultarGruposDeStock consultarGruposDeStock,
			MoverUnidades moverUnidades, ReabastecerGrupo reabastecerGrupo, ConsultarStockBajo consultarStockBajo,
			AjustarStock ajustarStock, TransferirStock transferirStock) {
		this.crearGrupoDeStock = crearGrupoDeStock;
		this.consultarGruposDeStock = consultarGruposDeStock;
		this.moverUnidades = moverUnidades;
		this.reabastecerGrupo = reabastecerGrupo;
		this.consultarStockBajo = consultarStockBajo;
		this.ajustarStock = ajustarStock;
		this.transferirStock = transferirStock;
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
				empresaId, request.sucursalId(), prendaId, combinacion, request.cantidadInicial()));
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

	@PostMapping("/api/v1/grupos-stock/{grupoId}/entrada")
	GrupoDeStockResponse reabastecer(@PathVariable UUID grupoId, @Valid @RequestBody EntradaDeStockRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return GrupoDeStockResponse.desde(reabastecerGrupo.ejecutar(empresaId, grupoId, request.cantidad()));
	}

	@PostMapping("/api/v1/grupos-stock/{grupoId}/transferir")
	GrupoDeStockResponse transferir(@PathVariable UUID grupoId, @Valid @RequestBody TransferirStockRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		GrupoDeStock origen = transferirStock.ejecutar(new TransferirStock.TransferirStockComando(
				empresaId, grupoId, request.sucursalDestinoId(), request.cantidad()));
		return GrupoDeStockResponse.desde(origen);
	}

	@PostMapping("/api/v1/grupos-stock/{grupoId}/ajuste")
	GrupoDeStockResponse ajustar(@PathVariable UUID grupoId, @Valid @RequestBody AjusteDeStockRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return GrupoDeStockResponse.desde(ajustarStock.ejecutar(new AjustarStock.AjustarStockComando(
				empresaId, grupoId, request.estado(), request.delta(), request.motivo())));
	}

	@GetMapping("/api/v1/grupos-stock/stock-bajo")
	List<GrupoDeStockResponse> stockBajo(@RequestParam(defaultValue = "1") int umbral,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarStockBajo.deEmpresa(UUID.fromString(empresaId), umbral).stream()
				.map(GrupoDeStockResponse::desde).toList();
	}
}
