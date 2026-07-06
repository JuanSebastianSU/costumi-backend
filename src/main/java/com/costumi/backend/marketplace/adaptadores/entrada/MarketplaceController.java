package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.aplicacion.DescubrirEmpresas;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
