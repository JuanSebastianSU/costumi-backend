package com.costumi.backend.catalogo.aplicacion;

import java.util.UUID;

/** Datos para crear una Categoría en la empresa del usuario autenticado. */
public record CrearCategoriaComando(UUID empresaId, String nombre) {
}
