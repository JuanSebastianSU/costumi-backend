package com.costumi.backend.disfraces.dominio;

/** Modo de un disfraz (RF-2.3, Capa 3). */
public enum ModoDeDisfraz {

	/** Unidad fija: indivisible y no personalizable; su stock es el de una prenda concreta. */
	UNIDAD_FIJA,

	/** Por partes: una lista de hasta 8 slots (secciones), cada uno con sus dos ejes. */
	POR_PARTES
}
