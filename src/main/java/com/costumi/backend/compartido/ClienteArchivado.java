package com.costumi.backend.compartido;

import java.util.UUID;

/**
 * El personal intentó operar (renta/venta) sobre una ficha de cliente <b>archivada</b> (R-E). Se traduce a
 * HTTP 409: para volver a operar con ese cliente, hay que reactivarlo primero. Vive en {@code compartido}
 * porque la usan varios módulos (Rentas, Ventas). No afecta el auto-checkout del marketplace.
 */
public class ClienteArchivado extends RuntimeException {

	public ClienteArchivado(UUID clienteId) {
		super("El cliente " + clienteId + " está archivado: reactivalo para poder operar con él");
	}
}
