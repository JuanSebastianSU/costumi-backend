package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.dominio.ResumenDeRentas;

/** DTO de salida con el conteo de rentas por bandeja, para las pestañas de G9. */
public record ResumenDeRentasResponse(long porEntregar, long activas, long vencidas, long cerradas) {

	static ResumenDeRentasResponse desde(ResumenDeRentas r) {
		return new ResumenDeRentasResponse(r.porEntregar(), r.activas(), r.vencidas(), r.cerradas());
	}
}
