package com.costumi.backend.caja.aplicacion;

import com.costumi.backend.caja.dominio.Turno;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista los turnos de la empresa (tenant). */
public interface ConsultarTurnos {

	/** Turnos de la empresa; {@code sucursalId} (opcional, de la sucursal activa) acota a esa sucursal. */
	List<Turno> deEmpresa(UUID empresaId, UUID sucursalId);
}
