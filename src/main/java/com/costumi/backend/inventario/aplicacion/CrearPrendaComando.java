package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.TipoArticulo;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para crear una Prenda en la empresa del usuario autenticado (RF-2.1, RF-2.10). */
public record CrearPrendaComando(UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
		BigDecimal precioRenta, BigDecimal precioVenta) {
}
