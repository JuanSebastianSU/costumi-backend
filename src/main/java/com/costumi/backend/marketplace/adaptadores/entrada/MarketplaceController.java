package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.aplicacion.DescubrirEmpresas;
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

	@GetMapping("/empresas")
	List<EmpresaVitrinaResponse> empresas(@RequestParam(name = "buscar", required = false) String buscar) {
		List<com.costumi.backend.marketplace.dominio.EmpresaEnVitrina> empresas = (buscar == null || buscar.isBlank())
				? descubrirEmpresas.activas()
				: descubrirEmpresas.buscar(buscar);
		return empresas.stream().map(EmpresaVitrinaResponse::desde).toList();
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
