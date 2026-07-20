package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ResumenDeIngresos;

import java.util.UUID;

/** Puerto de entrada: resumen de ingresos de una empresa (RF-9.1), scoped por tenant. */
public interface ConsultarIngresos {

	/** Ingresos de la empresa, o de una sola sucursal si {@code sucursalId} no es null. */
	ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId);
}
