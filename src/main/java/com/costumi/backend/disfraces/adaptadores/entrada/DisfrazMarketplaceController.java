package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.ConsultarDisfraces;
import com.costumi.backend.disfraces.aplicacion.ConsultarDisponibilidadDeDisfraz;
import com.costumi.backend.disfraces.dominio.Disfraz;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Vitrina pública de disfraces de una tienda (RF-18.3/18.4). Deja que el CLIENTE del marketplace
 * <b>descubra</b> los disfraces de una empresa y vea su estructura completa para personalizar (la
 * "ruleta"): por cada slot, si es fijo o personalizable, su eje de talla/prenda, la prenda fija o el
 * pool (categoría + valores de etiqueta permitidos), y si el disfraz está disponible.
 *
 * <p>Público (GET /marketplace/** es permitAll): lee por {@code empresaId} explícito, sin token.
 */
@RestController
@RequestMapping("/api/v1/marketplace/empresas/{empresaId}/disfraces")
class DisfrazMarketplaceController {

	private final ConsultarDisfraces consultarDisfraces;
	private final ConsultarDisponibilidadDeDisfraz consultarDisponibilidad;

	DisfrazMarketplaceController(ConsultarDisfraces consultarDisfraces,
			ConsultarDisponibilidadDeDisfraz consultarDisponibilidad) {
		this.consultarDisfraces = consultarDisfraces;
		this.consultarDisponibilidad = consultarDisponibilidad;
	}

	/** Lista los disfraces activos de la tienda (con su estructura de slots para la ruleta). */
	@GetMapping
	List<DisfrazResponse> listar(@PathVariable UUID empresaId) {
		return consultarDisfraces.activosDeEmpresa(empresaId).stream().map(DisfrazResponse::desde).toList();
	}

	/** Detalle de un disfraz activo: estructura completa + disponibilidad derivada (RF-2.4). */
	@GetMapping("/{disfrazId}")
	DisfrazDetalleResponse detalle(@PathVariable UUID empresaId, @PathVariable UUID disfrazId) {
		Disfraz disfraz = consultarDisfraces.activosDeEmpresa(empresaId).stream()
				.filter(d -> d.id().equals(disfrazId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disfraz no encontrado"));
		boolean disponible = consultarDisponibilidad.estaDisponible(empresaId, disfrazId);
		return new DisfrazDetalleResponse(DisfrazResponse.desde(disfraz), disponible);
	}

	/** Detalle público: la estructura del disfraz + si está disponible ahora. */
	record DisfrazDetalleResponse(DisfrazResponse disfraz, boolean disponible) {
	}
}
