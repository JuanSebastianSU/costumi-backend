package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;

/** Puerto de entrada: alta de un Tipo de etiqueta (RF-2.7.1). */
public interface CrearTipoEtiqueta {

	TipoEtiqueta ejecutar(CrearTipoEtiquetaComando comando);
}
