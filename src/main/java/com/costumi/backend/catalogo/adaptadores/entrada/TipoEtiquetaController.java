package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.aplicacion.AgregarValor;
import com.costumi.backend.catalogo.aplicacion.AgregarValorComando;
import com.costumi.backend.catalogo.aplicacion.ConsultarTiposEtiqueta;
import com.costumi.backend.catalogo.aplicacion.ConsultarValores;
import com.costumi.backend.catalogo.aplicacion.CrearTipoEtiqueta;
import com.costumi.backend.catalogo.aplicacion.CrearTipoEtiquetaComando;
import com.costumi.backend.catalogo.aplicacion.GestionarEtiquetas;
import com.costumi.backend.catalogo.aplicacion.RenombrarTipoEtiqueta;
import com.costumi.backend.catalogo.aplicacion.RenombrarValor;
import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiqueta;
import com.costumi.backend.compartido.ContextoDeTenant;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Motor de etiquetas: tipos (dimensiones) y sus valores, acotados al tenant del token (RF-2.7). */
@RestController
@RequestMapping("/api/v1/tipos-etiqueta")
class TipoEtiquetaController {

	private final CrearTipoEtiqueta crearTipoEtiqueta;
	private final ConsultarTiposEtiqueta consultarTiposEtiqueta;
	private final AgregarValor agregarValor;
	private final ConsultarValores consultarValores;
	private final RenombrarTipoEtiqueta renombrarTipoEtiqueta;
	private final RenombrarValor renombrarValor;
	private final GestionarEtiquetas gestionarEtiquetas;
	private final ContextoDeTenant tenant;

	TipoEtiquetaController(CrearTipoEtiqueta crearTipoEtiqueta, ConsultarTiposEtiqueta consultarTiposEtiqueta,
			AgregarValor agregarValor, ConsultarValores consultarValores, RenombrarTipoEtiqueta renombrarTipoEtiqueta,
			RenombrarValor renombrarValor, GestionarEtiquetas gestionarEtiquetas, ContextoDeTenant tenant) {
		this.crearTipoEtiqueta = crearTipoEtiqueta;
		this.consultarTiposEtiqueta = consultarTiposEtiqueta;
		this.agregarValor = agregarValor;
		this.consultarValores = consultarValores;
		this.renombrarTipoEtiqueta = renombrarTipoEtiqueta;
		this.renombrarValor = renombrarValor;
		this.gestionarEtiquetas = gestionarEtiquetas;
		this.tenant = tenant;
	}

	/** Archiva un tipo de etiqueta: deja de ofrecerse para etiquetar prendas nuevas, sin borrarlo. */
	@PostMapping("/{tipoId}/archivar")
	TipoEtiquetaResponse archivarTipo(@PathVariable UUID tipoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return TipoEtiquetaResponse.desde(gestionarEtiquetas.archivarTipo(empresaId, tipoId));
	}

	/** Reactiva un tipo de etiqueta archivado. */
	@PostMapping("/{tipoId}/activar")
	TipoEtiquetaResponse activarTipo(@PathVariable UUID tipoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return TipoEtiquetaResponse.desde(gestionarEtiquetas.activarTipo(empresaId, tipoId));
	}

	/** Archiva un valor de etiqueta: deja de ser elegible para prendas nuevas, sin borrarlo. */
	@PostMapping("/{tipoId}/valores/{valorId}/archivar")
	ValorEtiquetaResponse archivarValor(@PathVariable UUID tipoId, @PathVariable UUID valorId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return ValorEtiquetaResponse.desde(gestionarEtiquetas.archivarValor(empresaId, tipoId, valorId));
	}

	/** Reactiva un valor de etiqueta archivado. */
	@PostMapping("/{tipoId}/valores/{valorId}/activar")
	ValorEtiquetaResponse activarValor(@PathVariable UUID tipoId, @PathVariable UUID valorId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return ValorEtiquetaResponse.desde(gestionarEtiquetas.activarValor(empresaId, tipoId, valorId));
	}

	@PostMapping
	ResponseEntity<TipoEtiquetaResponse> crear(@Valid @RequestBody CrearTipoEtiquetaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		TipoEtiqueta tipo = crearTipoEtiqueta.ejecutar(new CrearTipoEtiquetaComando(empresaId, request.nombre(),
				request.defineVariante(), request.seleccionablePorCliente(), request.categoriasQueAplica()));
		URI location = uriBuilder.path("/api/v1/tipos-etiqueta/{id}").buildAndExpand(tipo.id()).toUri();
		return ResponseEntity.created(location).body(TipoEtiquetaResponse.desde(tipo));
	}

	@GetMapping
	List<TipoEtiquetaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarTiposEtiqueta.deEmpresa(UUID.fromString(empresaId)).stream()
				.map(TipoEtiquetaResponse::desde).toList();
	}

	@PostMapping("/{tipoId}/valores")
	ResponseEntity<ValorEtiquetaResponse> agregarValor(@PathVariable UUID tipoId,
			@Valid @RequestBody AgregarValorRequest request, @AuthenticationPrincipal Jwt jwt,
			UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		ValorEtiqueta valor = agregarValor.ejecutar(new AgregarValorComando(empresaId, tipoId, request.valor()));
		URI location = uriBuilder.path("/api/v1/tipos-etiqueta/{tipoId}/valores/{id}")
				.buildAndExpand(tipoId, valor.id()).toUri();
		return ResponseEntity.created(location).body(ValorEtiquetaResponse.desde(valor));
	}

	@GetMapping("/{tipoId}/valores")
	List<ValorEtiquetaResponse> listarValores(@PathVariable UUID tipoId, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarValores.deTipo(UUID.fromString(empresaId), tipoId).stream()
				.map(ValorEtiquetaResponse::desde).toList();
	}

	@PatchMapping("/{tipoId}")
	TipoEtiquetaResponse renombrar(@PathVariable UUID tipoId, @Valid @RequestBody RenombrarRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return TipoEtiquetaResponse.desde(renombrarTipoEtiqueta.ejecutar(empresaId, tipoId, request.nombre()));
	}

	@PatchMapping("/{tipoId}/valores/{valorId}")
	ValorEtiquetaResponse renombrarValor(@PathVariable UUID tipoId, @PathVariable UUID valorId,
			@Valid @RequestBody RenombrarRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return ValorEtiquetaResponse.desde(renombrarValor.ejecutar(empresaId, tipoId, valorId, request.nombre()));
	}
}
