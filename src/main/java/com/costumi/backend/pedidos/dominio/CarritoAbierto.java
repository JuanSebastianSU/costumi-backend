package com.costumi.backend.pedidos.dominio;

import java.util.UUID;

/**
 * Un carrito PENDIENTE del cliente en una tienda, para poder volver a él (RF-16.2/16.3).
 *
 * <p>El carrito es por <b>empresa × sucursal × tipo</b>, así que un cliente puede tener varios a la vez
 * en tiendas distintas. Sin esta vista no había forma de saber cuáles: la consulta de carrito exige
 * saber de antemano la empresa, la sucursal y el tipo, y el cliente no tenía dónde verlos.
 *
 * <p>Trae el nombre de la tienda y de la sucursal porque la lista es para elegir a cuál volver; con solo
 * los ids no se podría pintar.
 *
 * <p>Lleva las <b>unidades</b>, no el importe: el precio del carrito no está guardado, se calcula al
 * valorizarlo (renta = precio × cantidad × días, con la política de la tienda). Ponerlo aquí obligaría a
 * duplicar ese cálculo en SQL y a que las dos versiones se desincronizaran. El importe lo da el carrito
 * real al abrirlo.
 */
public record CarritoAbierto(UUID empresaId, String empresaNombre, UUID sucursalId, String sucursalNombre,
		TipoPedido tipo, int articulos) {
}
