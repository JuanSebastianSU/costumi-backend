package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.CarritoAbierto;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: los carritos que el cliente dejó abiertos, en cualquier tienda (RF-16.2/16.3).
 *
 * <p>Sin esto no había forma de volver a un carrito: consultarlo exige saber de antemano la empresa, la
 * sucursal y el tipo, y el cliente no tenía dónde verlos. En la práctica, al cambiar de tienda había que
 * agregar un artículo otra vez para que el carrito reapareciera.
 */
public interface ConsultarMisCarritos {

	List<CarritoAbierto> deUsuario(UUID usuarioId);
}
