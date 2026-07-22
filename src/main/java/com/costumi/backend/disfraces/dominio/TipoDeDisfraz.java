package com.costumi.backend.disfraces.dominio;

/**
 * Para qué está disponible un disfraz: solo RENTA, solo VENTA o AMBOS. Lo decide el <b>dueño</b> al armarlo
 * (RF-2.3) y acota lo que el cliente puede hacer: un disfraz de solo renta no se puede comprar y uno de solo
 * venta no se puede rentar. Es el equivalente al tipo de artículo de una prenda, pero del disfraz.
 */
public enum TipoDeDisfraz {

	RENTA,
	VENTA,
	AMBOS;

	public boolean permiteRenta() {
		return this != VENTA;
	}

	public boolean permiteVenta() {
		return this != RENTA;
	}
}
