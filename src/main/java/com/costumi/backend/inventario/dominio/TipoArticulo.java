package com.costumi.backend.inventario.dominio;

/** Un artículo puede ser solo renta, solo venta, o ambos (RF-2.1). */
public enum TipoArticulo {

	RENTA,
	VENTA,
	AMBOS;

	public boolean incluyeRenta() {
		return this == RENTA || this == AMBOS;
	}

	public boolean incluyeVenta() {
		return this == VENTA || this == AMBOS;
	}
}
