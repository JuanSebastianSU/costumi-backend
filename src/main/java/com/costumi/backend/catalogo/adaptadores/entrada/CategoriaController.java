package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.aplicacion.ConsultarCategorias;
import com.costumi.backend.catalogo.aplicacion.CrearCategoria;
import com.costumi.backend.catalogo.aplicacion.CrearCategoriaComando;
import com.costumi.backend.catalogo.aplicacion.GestionarCategoria;
import com.costumi.backend.catalogo.dominio.Categoria;
import com.costumi.backend.compartido.ContextoDeTenant;
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
 * Categorías del catálogo. El tenant es <b>implícito</b>: sale del {@link ContextoDeTenant} (claim
 * {@code empresa_id} del token), no de la ruta — así el usuario solo ve/crea lo de su empresa (§5.4).
 */
@RestController
@RequestMapping("/api/v1/categorias")
class CategoriaController {

	private final CrearCategoria crearCategoria;
	private final ConsultarCategorias consultarCategorias;
	private final GestionarCategoria gestionarCategoria;
	private final ContextoDeTenant tenant;

	CategoriaController(CrearCategoria crearCategoria, ConsultarCategorias consultarCategorias,
			GestionarCategoria gestionarCategoria, ContextoDeTenant tenant) {
		this.crearCategoria = crearCategoria;
		this.consultarCategorias = consultarCategorias;
		this.gestionarCategoria = gestionarCategoria;
		this.tenant = tenant;
	}

	/** Renombra una categoría (RF-2.7.6): propaga a todo lo que la usa. DUENO/ENCARGADO. */
	@PatchMapping("/{id}")
	CategoriaResponse renombrar(@PathVariable UUID id, @Valid @RequestBody RenombrarRequest request) {
		return CategoriaResponse.desde(gestionarCategoria.renombrar(tenant.empresaIdRequerida(), id, request.nombre()));
	}

	/** Archiva una categoría: deja de ofrecerse para clasificar/crear, sin borrarla. */
	@PostMapping("/{id}/archivar")
	CategoriaResponse archivar(@PathVariable UUID id) {
		return CategoriaResponse.desde(gestionarCategoria.archivar(tenant.empresaIdRequerida(), id));
	}

	/** Reactiva una categoría archivada. */
	@PostMapping("/{id}/activar")
	CategoriaResponse activar(@PathVariable UUID id) {
		return CategoriaResponse.desde(gestionarCategoria.activar(tenant.empresaIdRequerida(), id));
	}

	@PostMapping
	ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CrearCategoriaRequest request,
			UriComponentsBuilder uriBuilder) {
		Categoria categoria = crearCategoria.ejecutar(
				new CrearCategoriaComando(tenant.empresaIdRequerida(), request.nombre()));
		URI location = uriBuilder.path("/api/v1/categorias/{id}").buildAndExpand(categoria.id()).toUri();
		return ResponseEntity.created(location).body(CategoriaResponse.desde(categoria));
	}

	@GetMapping
	List<CategoriaResponse> listar() {
		return tenant.empresaId()
				.map(empresaId -> consultarCategorias.deEmpresa(empresaId).stream()
						.map(CategoriaResponse::desde).toList())
				.orElseGet(List::of);
	}
}
