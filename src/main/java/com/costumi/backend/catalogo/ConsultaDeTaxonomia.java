package com.costumi.backend.catalogo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * API pública del módulo Catálogo para que otros módulos <b>validen referencias a la taxonomía</b>
 * sin conocer sus clases internas (RF-2.7). Vive en el paquete base del módulo: es lo único que
 * Catálogo expone; el resto (dominio/aplicación/adaptadores) queda encapsulado.
 *
 * <p>La usa Inventario para comprobar que una {@code CombinacionDeVariante} referencia solo tipos
 * que <b>definen variante</b> y valores que <b>pertenecen a ese tipo</b>, todo acotado al tenant.
 */
public interface ConsultaDeTaxonomia {

	/** ¿La categoría existe y pertenece a la empresa? (validación de referencia cruzada por tenant, §5.4). */
	boolean categoriaExiste(UUID empresaId, UUID categoriaId);

	/** ¿El tipo existe en la empresa, no está archivado y sus valores definen variantes de stock? */
	boolean tipoDefineVariante(UUID empresaId, UUID tipoEtiquetaId);

	/** ¿El tipo (de la empresa) aplica a esa categoría? (un tipo sin categorías aplica a todas). */
	boolean tipoAplicaACategoria(UUID empresaId, UUID tipoEtiquetaId, UUID categoriaId);

	/** ¿El valor pertenece a ese tipo (de la empresa) y no está archivado? */
	boolean valorPerteneceATipo(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId);

	/** Nombres de tipo y valor de una etiqueta, para mostrarla como "Talla: M" en vez de dos UUID. */
	record EtiquetaConNombre(UUID tipoEtiquetaId, String tipoNombre, UUID valorEtiquetaId, String valorNombre) {
	}

	/**
	 * Resuelve los nombres (tipo y valor) de los valores de etiqueta dados, acotado al tenant e indexado por
	 * {@code valorEtiquetaId}. Ignora los ids que no existan o no sean de la empresa. La usa la "ruleta" de
	 * un slot (RF-2.3) para que el cliente vea "Talla: M" al comparar opciones y no un identificador. Resuelve
	 * la taxonomía de la empresa en una sola pasada (sin N+1 por opción).
	 */
	Map<UUID, EtiquetaConNombre> describirValores(UUID empresaId, Collection<UUID> valorEtiquetaIds);
}
