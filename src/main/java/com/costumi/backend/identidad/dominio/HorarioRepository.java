package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: horario de atención de una tienda (A7), scoped por empresa. */
public interface HorarioRepository {

	/** El horario de la empresa (una franja por día que abre), ordenado por día. Vacío si no cargó ninguno. */
	List<HorarioDeDia> deEmpresa(UUID empresaId);

	/** Reemplaza el horario completo de la empresa por el dado (borra los días que ya no estén). */
	void reemplazar(UUID empresaId, List<HorarioDeDia> horario);
}
