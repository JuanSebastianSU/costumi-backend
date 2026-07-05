package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.aplicacion.ConsultarCategorias;
import com.costumi.backend.catalogo.aplicacion.CrearCategoria;
import com.costumi.backend.catalogo.aplicacion.CrearCategoriaComando;
import com.costumi.backend.catalogo.dominio.Categoria;
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

/**
 * Categorías del catálogo. El tenant es <b>implícito</b>: sale del claim {@code empresa_id} del
 * token, no de la ruta — así el usuario solo ve/crea lo de su empresa (aislamiento multi-tenant).
 */
@RestController
@RequestMapping("/api/v1/categorias")
class CategoriaController {

	private final CrearCategoria crearCategoria;
	private final ConsultarCategorias consultarCategorias;

	CategoriaController(CrearCategoria crearCategoria, ConsultarCategorias consultarCategorias) {
		this.crearCategoria = crearCategoria;
		this.consultarCategorias = consultarCategorias;
	}

	@PostMapping
	ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CrearCategoriaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Categoria categoria = crearCategoria.ejecutar(new CrearCategoriaComando(empresaId, request.nombre()));
		URI location = uriBuilder.path("/api/v1/categorias/{id}").buildAndExpand(categoria.id()).toUri();
		return ResponseEntity.created(location).body(CategoriaResponse.desde(categoria));
	}

	@GetMapping
	List<CategoriaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of(); // un usuario de plataforma (SuperAdmin) no tiene catálogo propio
		}
		return consultarCategorias.deEmpresa(UUID.fromString(empresaId)).stream()
				.map(CategoriaResponse::desde).toList();
	}
}
