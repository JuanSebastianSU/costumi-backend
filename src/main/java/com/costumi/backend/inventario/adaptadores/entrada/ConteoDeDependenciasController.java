package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.inventario.ConsultaDeInventario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Conteo de dependencias de la taxonomía (RF-2.8/2.7.6): cuántas prendas activas referencian una
 * categoría o una etiqueta. La UI lo consulta para confirmar el impacto <b>antes</b> de archivar.
 *
 * <p>Vive en Inventario (no en Catálogo) a propósito: el dato es "cuántas prendas", un concepto de
 * inventario. Ponerlo en Catálogo obligaría a que Catálogo dependiera de Inventario, cerrando un ciclo
 * entre módulos que Spring Modulith prohíbe (Inventario ya depende de Catálogo por la taxonomía).
 *
 * <p>Todo se acota al tenant del token: {@link ConsultaDeInventario} filtra por {@code empresaId}, de modo
 * que una categoría/etiqueta de otra empresa devuelve 0 (no fuga de datos cruzados, §5.4).
 */
@RestController
class ConteoDeDependenciasController {

	private final ConsultaDeInventario consultaDeInventario;
	private final ContextoDeTenant tenant;

	ConteoDeDependenciasController(ConsultaDeInventario consultaDeInventario, ContextoDeTenant tenant) {
		this.consultaDeInventario = consultaDeInventario;
		this.tenant = tenant;
	}

	/** Prendas activas que pertenecen a la categoría (impacto de archivarla). DUENO/ENCARGADO. */
	@GetMapping("/api/v1/categorias/{categoriaId}/prendas/conteo")
	ConteoDeDependenciasResponse deCategoria(@PathVariable UUID categoriaId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return new ConteoDeDependenciasResponse(consultaDeInventario.contarPrendasEnCategoria(empresaId, categoriaId));
	}

	/** Prendas activas que llevan alguna etiqueta de ese tipo (impacto de archivar el tipo). DUENO/ENCARGADO. */
	@GetMapping("/api/v1/tipos-etiqueta/{tipoId}/prendas/conteo")
	ConteoDeDependenciasResponse deTipoEtiqueta(@PathVariable UUID tipoId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return new ConteoDeDependenciasResponse(consultaDeInventario.contarPrendasConTipoEtiqueta(empresaId, tipoId));
	}

	/** Prendas activas que llevan ese valor de etiqueta (impacto de archivar el valor). DUENO/ENCARGADO. */
	@GetMapping("/api/v1/tipos-etiqueta/{tipoId}/valores/{valorId}/prendas/conteo")
	ConteoDeDependenciasResponse deValorEtiqueta(@PathVariable UUID tipoId, @PathVariable UUID valorId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return new ConteoDeDependenciasResponse(consultaDeInventario.contarPrendasConValorEtiqueta(empresaId, valorId));
	}
}
