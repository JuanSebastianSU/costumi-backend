package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

/** Puerto de entrada: alta de un Disfraz (Capa 3, RF-2.3). */
public interface CrearDisfraz {

	Disfraz ejecutar(CrearDisfrazComando comando);
}
