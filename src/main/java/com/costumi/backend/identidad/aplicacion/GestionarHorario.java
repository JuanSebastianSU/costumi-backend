package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.HorarioDeDia;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: el Dueño consulta y fija el horario de atención de su tienda (A7). */
public interface GestionarHorario {

	List<HorarioDeDia> deEmpresa(UUID empresaId);

	/** Fija el horario completo (reemplaza el anterior). Rechaza días repetidos. */
	void fijar(UUID empresaId, List<HorarioDeDia> horario);
}
