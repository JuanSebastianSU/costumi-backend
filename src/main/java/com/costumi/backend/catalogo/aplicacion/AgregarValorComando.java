package com.costumi.backend.catalogo.aplicacion;

import java.util.UUID;

/** Datos para agregar un Valor a un Tipo de etiqueta de la empresa. */
public record AgregarValorComando(UUID empresaId, UUID tipoEtiquetaId, String valor) {
}
