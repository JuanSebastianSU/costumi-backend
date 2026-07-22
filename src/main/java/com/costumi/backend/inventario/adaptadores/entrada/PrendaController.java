package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.compartido.RespuestaPaginada;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.inventario.aplicacion.AsignarFotoDePrenda;
import com.costumi.backend.inventario.aplicacion.CambiarEstadoPrenda;
import com.costumi.backend.inventario.aplicacion.ConsultarCatalogo;
import com.costumi.backend.inventario.aplicacion.ConsultarPrendas;
import com.costumi.backend.inventario.aplicacion.CrearPrenda;
import com.costumi.backend.inventario.aplicacion.CrearPrendaComando;
import com.costumi.backend.inventario.aplicacion.EditarPrenda;
import com.costumi.backend.inventario.aplicacion.EditarPrendaComando;
import com.costumi.backend.inventario.aplicacion.EtiquetaSeleccionada;
import com.costumi.backend.inventario.dominio.Prenda;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Biblioteca de prendas del inventario, acotada al tenant del token (RF-2). */
@RestController
@RequestMapping("/api/v1/prendas")
class PrendaController {

	private final CrearPrenda crearPrenda;
	private final EditarPrenda editarPrenda;
	private final CambiarEstadoPrenda cambiarEstadoPrenda;
	private final ConsultarPrendas consultarPrendas;
	private final ConsultarCatalogo consultarCatalogo;
	private final AsignarFotoDePrenda asignarFotoDePrenda;
	private final ContextoDeTenant tenant;

	PrendaController(CrearPrenda crearPrenda, EditarPrenda editarPrenda, CambiarEstadoPrenda cambiarEstadoPrenda,
			ConsultarPrendas consultarPrendas, ConsultarCatalogo consultarCatalogo,
			AsignarFotoDePrenda asignarFotoDePrenda, ContextoDeTenant tenant) {
		this.crearPrenda = crearPrenda;
		this.editarPrenda = editarPrenda;
		this.cambiarEstadoPrenda = cambiarEstadoPrenda;
		this.consultarPrendas = consultarPrendas;
		this.consultarCatalogo = consultarCatalogo;
		this.asignarFotoDePrenda = asignarFotoDePrenda;
		this.tenant = tenant;
	}

	/** Edita una prenda (RF-2.10): nombre, precios, valores y etiquetas. DUENO/ENCARGADO/BODEGA. */
	@PutMapping("/{id}")
	PrendaResponse editar(@PathVariable UUID id, @Valid @RequestBody EditarPrendaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<EtiquetaSeleccionada> etiquetas = request.etiquetas().stream()
				.map(e -> new EtiquetaSeleccionada(e.tipoEtiquetaId(), e.valorEtiquetaId()))
				.toList();
		Prenda prenda = editarPrenda.ejecutar(new EditarPrendaComando(empresaId, id, request.nombre(),
				request.precioRenta(), request.precioVenta(), request.costoAdquisicion(), request.depositoSugerido(),
				request.valorReposicion(), request.valorDano(), etiquetas));
		return PrendaResponse.desde(prenda);
	}

	/** Archiva una prenda: la retira de la operación (renta/venta/pool) sin borrarla. */
	@PostMapping("/{id}/archivar")
	PrendaResponse archivar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return PrendaResponse.desde(cambiarEstadoPrenda.archivar(empresaId, id));
	}

	/** Reactiva una prenda archivada. */
	@PostMapping("/{id}/activar")
	PrendaResponse activar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return PrendaResponse.desde(cambiarEstadoPrenda.activar(empresaId, id));
	}

	/** Sube/actualiza la foto de una prenda (RF-2.9, multipart). */
	@PostMapping("/{id}/foto")
	ResponseEntity<PrendaResponse> subirFoto(@PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo,
			@AuthenticationPrincipal Jwt jwt) throws IOException {
		UUID empresaId = tenant.empresaIdRequerida();
		if (archivo == null || archivo.isEmpty()) {
			throw new IllegalArgumentException("El archivo de la foto es obligatorio");
		}
		Prenda prenda = asignarFotoDePrenda.ejecutar(empresaId, id, archivo.getBytes());
		return ResponseEntity.ok(PrendaResponse.desde(prenda));
	}

	@PostMapping
	ResponseEntity<PrendaResponse> crear(@Valid @RequestBody CrearPrendaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<EtiquetaSeleccionada> etiquetas = request.etiquetas().stream()
				.map(e -> new EtiquetaSeleccionada(e.tipoEtiquetaId(), e.valorEtiquetaId()))
				.toList();
		Prenda prenda = crearPrenda.ejecutar(new CrearPrendaComando(empresaId, request.categoriaId(),
				request.nombre(), request.tipoArticulo(), request.precioRenta(), request.precioVenta(),
				request.costoAdquisicion(), request.depositoSugerido(), request.valorReposicion(),
				request.valorDano(), etiquetas));
		URI location = uriBuilder.path("/api/v1/prendas/{id}").buildAndExpand(prenda.id()).toUri();
		return ResponseEntity.created(location).body(PrendaResponse.desde(prenda));
	}

	@GetMapping
	RespuestaPaginada<PrendaResponse> listar(@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Integer pagina,
			@RequestParam(required = false) Integer tamano, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return new RespuestaPaginada<>(List.of(), 0, 0, 0, 0);
		}
		return RespuestaPaginada.desde(
				consultarPrendas.listar(UUID.fromString(empresaId), buscar, SolicitudDePagina.de(pagina, tamano)),
				PrendaResponse::desde);
	}

	/**
	 * Catálogo del dueño para verlo por categoría y filtrar (RF-2/RF-13), y para elegir opciones de una
	 * parte de un disfraz. {@code categoriaId} filtra por categoría; {@code etiqueta} es una lista de pares
	 * {@code "tipoEtiquetaId:valorEtiquetaId"} (repetible): AND entre dimensiones, OR entre valores de la
	 * misma dimensión. Cada prenda vuelve con su stock disponible y sus etiquetas.
	 */
	@GetMapping("/catalogo")
	List<PrendaDeCatalogoResponse> catalogo(@RequestParam(required = false) UUID categoriaId,
			@RequestParam(name = "etiqueta", required = false) List<String> etiquetas,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return List.of();
		}
		return consultarCatalogo.ejecutar(UUID.fromString(empresaId), categoriaId, parsearEtiquetas(etiquetas))
				.stream().map(PrendaDeCatalogoResponse::desde).toList();
	}

	/** Convierte los pares "tipo:valor" del query a un mapa {@code tipoEtiquetaId -> valores permitidos}. */
	private static Map<UUID, Set<UUID>> parsearEtiquetas(List<String> etiquetas) {
		Map<UUID, Set<UUID>> filtros = new LinkedHashMap<>();
		if (etiquetas != null) {
			for (String par : etiquetas) {
				String[] partes = par.split(":", 2);
				if (partes.length == 2) {
					try {
						UUID tipo = UUID.fromString(partes[0].trim());
						UUID valor = UUID.fromString(partes[1].trim());
						filtros.computeIfAbsent(tipo, k -> new LinkedHashSet<>()).add(valor);
					} catch (IllegalArgumentException ignorado) {
						// Par mal formado: se ignora en vez de romper la consulta.
					}
				}
			}
		}
		return filtros;
	}
}
