package com.costumi.backend.caja.aplicacion;

import com.costumi.backend.caja.dominio.Turno;

/** Puerto de entrada: registra un movimiento (ingreso/egreso) en un turno abierto (RF-6.3). */
public interface RegistrarMovimiento {

	Turno ejecutar(RegistrarMovimientoComando comando);
}
