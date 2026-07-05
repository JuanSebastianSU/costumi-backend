package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.rentas.dominio.Renta;

/** Puerto de entrada: crear una Renta (RF-3.1). */
public interface CrearRenta {

	Renta ejecutar(CrearRentaComando comando);
}
