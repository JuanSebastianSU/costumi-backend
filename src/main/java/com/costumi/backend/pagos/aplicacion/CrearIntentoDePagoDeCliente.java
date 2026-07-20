package com.costumi.backend.pagos.aplicacion;

/**
 * Puerto de entrada: un CLIENTE del marketplace inicia el pago en línea de <b>su propia</b> venta o renta desde
 * su cuenta (RF-6.11/14.4). A diferencia del intento del personal (que saca la empresa del token), aquí el
 * cliente indica la tienda y se verifica que la operación sea suya antes de crear el checkout. El monto sigue
 * validándose contra el total pendiente real (pago total de golpe, sin adelantos).
 */
public interface CrearIntentoDePagoDeCliente {

	CrearIntentoDePago.Resultado ejecutar(CrearIntentoDePagoDeClienteComando comando);
}
