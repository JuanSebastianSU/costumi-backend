package com.costumi.backend.catalogo;

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

	/** ¿El tipo existe en la empresa, no está archivado y sus valores definen variantes de stock? */
	boolean tipoDefineVariante(UUID empresaId, UUID tipoEtiquetaId);

	/** ¿El valor pertenece a ese tipo (de la empresa) y no está archivado? */
	boolean valorPerteneceATipo(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId);
}
