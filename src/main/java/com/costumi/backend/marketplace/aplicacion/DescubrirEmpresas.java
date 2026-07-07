package com.costumi.backend.marketplace.aplicacion;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: descubrir las empresas ACTIVAS del marketplace (RF-18.1, RF-15.6). */
public interface DescubrirEmpresas {

	List<EmpresaEnVitrina> activas();

	/** Empresas ACTIVAS que coinciden con el texto de búsqueda (RF-18.1). */
	List<EmpresaEnVitrina> buscar(String texto);

	/** Catálogo público de una tienda (empresa ACTIVA). Vacío si no existe o no está activa. */
	List<PrendaEnVitrina> catalogo(UUID empresaId);
}
