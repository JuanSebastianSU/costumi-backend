package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.Carrito;

/** Puerto de entrada: agrega un ítem al carrito (lo crea si no existe) (RF-16). */
public interface AgregarItemAlCarrito {

	Carrito ejecutar(AgregarItemAlCarritoComando comando);
}
