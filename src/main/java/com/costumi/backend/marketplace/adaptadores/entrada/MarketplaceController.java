package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.aplicacion.DescubrirEmpresas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Marketplace del cliente (RF-18): descubrimiento público de empresas ACTIVAS. */
@RestController
@RequestMapping("/api/v1/marketplace")
class MarketplaceController {

	private final DescubrirEmpresas descubrirEmpresas;

	MarketplaceController(DescubrirEmpresas descubrirEmpresas) {
		this.descubrirEmpresas = descubrirEmpresas;
	}

	/** Vitrina de tiendas ACTIVAS, paginada (RF-18.1). {@code buscar} filtra por nombre. */
	@GetMapping("/empresas")
	com.costumi.backend.compartido.RespuestaPaginada<EmpresaVitrinaResponse> empresas(
			@RequestParam(name = "buscar", required = false) String buscar,
			@RequestParam(name = "pagina", required = false) Integer pagina,
			@RequestParam(name = "tamano", required = false) Integer tamano) {
		return com.costumi.backend.compartido.RespuestaPaginada.desde(
				descubrirEmpresas.empresas(buscar, com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano)),
				EmpresaVitrinaResponse::desde);
	}

	/** Carrusel de disfraces destacados del marketplace (C1), cruzando tiendas ACTIVAS. Público. */
	@GetMapping("/destacados")
	List<DisfrazDestacadoResponse> destacados(@RequestParam(name = "limite", defaultValue = "10") int limite) {
		return descubrirEmpresas.destacados(limite).stream().map(DisfrazDestacadoResponse::desde).toList();
	}

	/** Detalle público de UNA tienda ACTIVA (cabecera de su vitrina): logo/portada/ciudad/#disfraces (RF-18.1). */
	@GetMapping("/empresas/{empresaId}")
	ResponseEntity<EmpresaVitrinaResponse> empresa(@PathVariable UUID empresaId) {
		return descubrirEmpresas.empresa(empresaId).map(EmpresaVitrinaResponse::desde)
				.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	/** Facetas: categorías presentes en el catálogo público de la tienda (para filtrar, RF-18.1). */
	@GetMapping("/empresas/{empresaId}/categorias")
	List<CategoriaVitrinaResponse> categorias(@PathVariable UUID empresaId) {
		return descubrirEmpresas.categoriasDe(empresaId).stream().map(CategoriaVitrinaResponse::desde).toList();
	}

	/** Horario de atención público de la tienda (A7): el cliente ve si está abierta y a qué hora cierra. */
	@GetMapping("/empresas/{empresaId}/horario")
	List<HorarioVitrinaResponse> horario(@PathVariable UUID empresaId) {
		return descubrirEmpresas.horarioDe(empresaId).stream().map(HorarioVitrinaResponse::desde).toList();
	}

	/** Catálogo público de una tienda: el cliente ve las prendas de cualquier empresa ACTIVA (RF-18). */
	/** Catálogo público de una tienda; con {@code categoria} (id) filtra por esa categoría (RF-18.1). */
	@GetMapping("/empresas/{empresaId}/catalogo")
	List<PrendaVitrinaResponse> catalogo(@PathVariable UUID empresaId,
			@RequestParam(name = "categoria", required = false) UUID categoria) {
		return descubrirEmpresas.catalogo(empresaId, categoria).stream().map(PrendaVitrinaResponse::desde).toList();
	}

	/**
	 * Sucursales (puntos de retiro) públicas de una tienda ACTIVA (RF-18.5): el cliente del
	 * marketplace elige en cuál retirar antes de armar su carrito. Sin token, como el resto de la vitrina.
	 */
	@GetMapping("/empresas/{empresaId}/sucursales")
	List<SucursalVitrinaResponse> sucursales(@PathVariable UUID empresaId) {
		return descubrirEmpresas.sucursales(empresaId).stream().map(SucursalVitrinaResponse::desde).toList();
	}
}
