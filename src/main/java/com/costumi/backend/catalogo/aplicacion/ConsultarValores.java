package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.ValorEtiqueta;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista los Valores de un Tipo de etiqueta de la empresa. */
public interface ConsultarValores {

	List<ValorEtiqueta> deTipo(UUID empresaId, UUID tipoEtiquetaId);
}
