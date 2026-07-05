package com.costumi.backend.pedidos.aplicacion;

/** No hay carrito pendiente para esa combinación de cliente/sucursal/tipo. */
public class CarritoNoEncontrado extends RuntimeException {

	public CarritoNoEncontrado() {
		super("No hay un carrito pendiente para esa combinación");
	}
}
