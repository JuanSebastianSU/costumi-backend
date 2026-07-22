package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.disfraces.aplicacion.GestionCategoriasDeDisfraz;
import com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

/**
 * Categorías de DISFRAZ (RF-2.3): taxonomía propia del dueño para agrupar sus disfraces (ej. "Piratas"),
 * separada de las categorías de prenda. El tenant es implícito (claim {@code empresa_id} del token, §5.4).
 */
@RestController
@RequestMapping("/api/v1/disfraces/categorias")
class CategoriaDeDisfrazController {

	private final GestionCategoriasDeDisfraz categorias;
	private final ContextoDeTenant tenant;

	CategoriaDeDisfrazController(GestionCategoriasDeDisfraz categorias, ContextoDeTenant tenant) {
		this.categorias = categorias;
		this.tenant = tenant;
	}

	@GetMapping
	List<CategoriaDeDisfrazResponse> listar() {
		return tenant.empresaId()
				.map(empresaId -> categorias.deEmpresa(empresaId).stream()
						.map(CategoriaDeDisfrazResponse::desde).toList())
				.orElseGet(List::of);
	}

	@PostMapping
	ResponseEntity<CategoriaDeDisfrazResponse> crear(@Valid @RequestBody CategoriaDeDisfrazRequest request,
			UriComponentsBuilder uriBuilder) {
		CategoriaDeDisfraz categoria = categorias.crear(tenant.empresaIdRequerida(), request.nombre());
		URI location = uriBuilder.path("/api/v1/disfraces/categorias/{id}").buildAndExpand(categoria.id()).toUri();
		return ResponseEntity.created(location).body(CategoriaDeDisfrazResponse.desde(categoria));
	}

	@PatchMapping("/{id}")
	CategoriaDeDisfrazResponse renombrar(@PathVariable UUID id, @Valid @RequestBody CategoriaDeDisfrazRequest request) {
		return CategoriaDeDisfrazResponse.desde(
				categorias.renombrar(tenant.empresaIdRequerida(), id, request.nombre()));
	}

	@PostMapping("/{id}/archivar")
	CategoriaDeDisfrazResponse archivar(@PathVariable UUID id) {
		return CategoriaDeDisfrazResponse.desde(categorias.archivar(tenant.empresaIdRequerida(), id));
	}

	@PostMapping("/{id}/activar")
	CategoriaDeDisfrazResponse activar(@PathVariable UUID id) {
		return CategoriaDeDisfrazResponse.desde(categorias.activar(tenant.empresaIdRequerida(), id));
	}
}
