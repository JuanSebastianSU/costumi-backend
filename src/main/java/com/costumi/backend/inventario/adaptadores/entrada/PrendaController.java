package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.aplicacion.AsignarFotoDePrenda;
import com.costumi.backend.inventario.aplicacion.ConsultarPrendas;
import com.costumi.backend.inventario.aplicacion.CrearPrenda;
import com.costumi.backend.inventario.aplicacion.CrearPrendaComando;
import com.costumi.backend.inventario.aplicacion.EtiquetaSeleccionada;
import com.costumi.backend.inventario.dominio.Prenda;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Biblioteca de prendas del inventario, acotada al tenant del token (RF-2). */
@RestController
@RequestMapping("/api/v1/prendas")
class PrendaController {

	private final CrearPrenda crearPrenda;
	private final ConsultarPrendas consultarPrendas;
	private final AsignarFotoDePrenda asignarFotoDePrenda;

	PrendaController(CrearPrenda crearPrenda, ConsultarPrendas consultarPrendas,
			AsignarFotoDePrenda asignarFotoDePrenda) {
		this.crearPrenda = crearPrenda;
		this.consultarPrendas = consultarPrendas;
		this.asignarFotoDePrenda = asignarFotoDePrenda;
	}

	/** Sube/actualiza la foto de una prenda (RF-2.9, multipart). */
	@PostMapping("/{id}/foto")
	ResponseEntity<PrendaResponse> subirFoto(@PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo,
			@AuthenticationPrincipal Jwt jwt) throws IOException {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		if (archivo == null || archivo.isEmpty()) {
			throw new IllegalArgumentException("El archivo de la foto es obligatorio");
		}
		Prenda prenda = asignarFotoDePrenda.ejecutar(empresaId, id, archivo.getBytes(),
				archivo.getContentType(), archivo.getOriginalFilename());
		return ResponseEntity.ok(PrendaResponse.desde(prenda));
	}

	@PostMapping
	ResponseEntity<PrendaResponse> crear(@Valid @RequestBody CrearPrendaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		List<EtiquetaSeleccionada> etiquetas = request.etiquetas().stream()
				.map(e -> new EtiquetaSeleccionada(e.tipoEtiquetaId(), e.valorEtiquetaId()))
				.toList();
		Prenda prenda = crearPrenda.ejecutar(new CrearPrendaComando(empresaId, request.categoriaId(),
				request.nombre(), request.tipoArticulo(), request.precioRenta(), request.precioVenta(),
				request.costoAdquisicion(), request.depositoSugerido(), etiquetas));
		URI location = uriBuilder.path("/api/v1/prendas/{id}").buildAndExpand(prenda.id()).toUri();
		return ResponseEntity.created(location).body(PrendaResponse.desde(prenda));
	}

	@GetMapping
	List<PrendaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarPrendas.deEmpresa(UUID.fromString(empresaId)).stream().map(PrendaResponse::desde).toList();
	}
}
