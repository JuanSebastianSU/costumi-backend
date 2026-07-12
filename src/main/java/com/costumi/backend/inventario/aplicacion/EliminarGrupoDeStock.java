package com.costumi.backend.inventario.aplicacion;

import java.util.UUID;

/**
 * Puerto de entrada: borra <b>físicamente</b> un grupo de stock (R-F), solo si es seguro: el grupo debe
 * estar vacío (0 unidades) y no ser el último grupo de su prenda en la sucursal (para que las devoluciones
 * siempre tengan dónde reingresar). Para retirar stock con historial se usa el ajuste/archivado, no esto.
 */
public interface EliminarGrupoDeStock {

	void ejecutar(UUID empresaId, UUID grupoId);
}
