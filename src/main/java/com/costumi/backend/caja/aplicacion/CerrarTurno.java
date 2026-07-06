package com.costumi.backend.caja.aplicacion;

import com.costumi.backend.caja.dominio.Turno;

import java.math.BigDecimal;
import java.util.UUID;

/** Puerto de entrada: cierra un turno con el efectivo contado (corte y cuadre, RF-6.10). */
public interface CerrarTurno {

	Turno ejecutar(UUID empresaId, UUID turnoId, BigDecimal efectivoContado);
}
