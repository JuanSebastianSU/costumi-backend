package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.ConsultarDisfraces;
import com.costumi.backend.disfraces.aplicacion.GestionCategoriasDeDisfraz;
import com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz;
import com.costumi.backend.disfraces.aplicacion.ConsultarDisponibilidadDeDisfraz;
import com.costumi.backend.disfraces.aplicacion.ConsultarOpcionesDeSlot;
import com.costumi.backend.disfraces.dominio.Disfraz;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	private final ConsultarOpcionesDeSlot consultarOpcionesDeSlot;
	private final GestionCategoriasDeDisfraz categorias;

	DisfrazMarketplaceController(ConsultarDisfraces consultarDisfraces,
			ConsultarDisponibilidadDeDisfraz consultarDisponibilidad,
			ConsultarOpcionesDeSlot consultarOpcionesDeSlot, GestionCategoriasDeDisfraz categorias) {
		this.consultarDisfraces = consultarDisfraces;
		this.consultarDisponibilidad = consultarDisponibilidad;
		this.consultarOpcionesDeSlot = consultarOpcionesDeSlot;
		this.categorias = categorias;
	}

	/** Nombres de las categorías de la tienda, en UNA consulta para toda la vitrina (sin N+1). */
	private java.util.Map<UUID, String> nombresDeCategoria(UUID empresaId) {
		return categorias.deEmpresa(empresaId).stream()
				.collect(java.util.stream.Collectors.toMap(CategoriaDeDisfraz::id, CategoriaDeDisfraz::nombre));
	}

	/** Lista los disfraces activos de la tienda (con su estructura de slots y su precio de renta sugerido). */
	@GetMapping
	List<DisfrazResponse> listar(@PathVariable UUID empresaId) {
		List<Disfraz> disfraces = consultarDisfraces.activosDeEmpresa(empresaId);
		// Sugeridos de toda la vitrina en un solo cálculo (catálogo cargado una vez): sin N+1.
		java.util.Map<UUID, ConsultarDisfraces.Sugeridos> sugeridos = consultarDisfraces.sugeridosDe(empresaId, disfraces);
		java.util.Map<UUID, String> nombres = nombresDeCategoria(empresaId);
		return disfraces.stream()
				.map(d -> DisfrazResponse.desde(d, sugeridos.get(d.id()), nombres.get(d.categoriaId())))
				.toList();
	}

	/** Detalle de un disfraz activo: estructura completa + precio sugerido + disponibilidad derivada (RF-2.4). */
	@GetMapping("/{disfrazId}")
	DisfrazDetalleResponse detalle(@PathVariable UUID empresaId, @PathVariable UUID disfrazId) {
		Disfraz disfraz = consultarDisfraces.activosDeEmpresa(empresaId).stream()
				.filter(d -> d.id().equals(disfrazId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disfraz no encontrado"));
		boolean disponible = consultarDisponibilidad.estaDisponible(empresaId, disfrazId);
		DisfrazResponse resp = DisfrazResponse.desde(disfraz, consultarDisfraces.sugeridosDe(empresaId, disfraz),
				nombresDeCategoria(empresaId).get(disfraz.categoriaId()));
		return new DisfrazDetalleResponse(resp, disponible);
	}

	/**
	 * "Ruleta" de un slot: las opciones concretas disponibles que el cliente puede elegir (con su stock y
	 * precio). {@code valores} (opcional, repetible) acota por valores de etiqueta (talla/color/modelo).
	 */
	@GetMapping("/{disfrazId}/slots/{orden}/opciones")
	SlotOpcionesResponse opcionesDeSlot(@PathVariable UUID empresaId, @PathVariable UUID disfrazId,
			@PathVariable int orden, @RequestParam(name = "valores", required = false) List<UUID> valores) {
		return SlotOpcionesResponse.desde(consultarOpcionesDeSlot.opciones(empresaId, disfrazId, orden, valores));
	}

	/** Detalle público: la estructura del disfraz + si está disponible ahora. */
	record DisfrazDetalleResponse(DisfrazResponse disfraz, boolean disponible) {
	}
}
