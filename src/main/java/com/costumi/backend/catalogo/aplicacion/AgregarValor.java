package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.ValorEtiqueta;

/** Puerto de entrada: agrega un Valor a un Tipo de etiqueta (RF-2.7.1). */
public interface AgregarValor {

	ValorEtiqueta ejecutar(AgregarValorComando comando);
}
