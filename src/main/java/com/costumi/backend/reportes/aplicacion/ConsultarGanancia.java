package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ResumenDeGanancia;

import java.util.UUID;

/** Puerto de entrada: consulta la ganancia de la empresa (ingreso − costo, RF-9). */
public interface ConsultarGanancia {

	/** Ganancia de la empresa, o de una sola sucursal si {@code sucursalId} no es null. */
	ResumenDeGanancia gananciaDeEmpresa(UUID empresaId, UUID sucursalId);
}
