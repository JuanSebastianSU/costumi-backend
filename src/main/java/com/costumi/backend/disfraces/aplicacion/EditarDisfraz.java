package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

/** Puerto de entrada: edición de un Disfraz existente (RF-2.3), acotada al tenant. */
public interface EditarDisfraz {

	Disfraz ejecutar(EditarDisfrazComando comando);
}
