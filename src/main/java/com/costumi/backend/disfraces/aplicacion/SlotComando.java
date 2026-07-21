package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;

import java.util.List;
import java.util.UUID;

/**
 * Datos de un slot al crear/editar un disfraz. Si el eje es personalizable, sus opciones se definen con
 * {@code prendasOpcion} (prendas explícitas elegidas del inventario) o con {@code pool} (categoría +
 * etiquetas), de forma excluyente. {@code prendaFijaId} solo aplica al eje fijo.
 */
public record SlotComando(int orden, String nombre, EjeDePrenda ejePrenda, UUID prendaFijaId, PoolComando pool,
		List<UUID> prendasOpcion, boolean opcional) {

	public SlotComando {
		prendasOpcion = (prendasOpcion == null) ? List.of() : List.copyOf(prendasOpcion);
	}
}
