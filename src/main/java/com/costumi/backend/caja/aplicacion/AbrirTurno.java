package com.costumi.backend.caja.aplicacion;

import com.costumi.backend.caja.dominio.Turno;

/** Puerto de entrada: abre un turno de caja con su fondo inicial (RF-6.3). */
public interface AbrirTurno {

	Turno ejecutar(AbrirTurnoComando comando);
}
