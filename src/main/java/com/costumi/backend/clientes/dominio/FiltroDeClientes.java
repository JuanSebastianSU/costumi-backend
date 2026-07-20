package com.costumi.backend.clientes.dominio;

/**
 * Categorías de "pendientes" por las que se puede filtrar la lista de clientes (RF-11.5/11.6):
 * <ul>
 *   <li>{@code PENDIENTES}: indicador general — tiene rentas activas por devolver o algún saldo por cobrar.</li>
 *   <li>{@code VENCIDAS}: tiene una renta activa cuya fecha de devolución ya pasó.</li>
 *   <li>{@code MULTAS}: incurrió en alguna multa (daños/retraso que superaron el depósito), pagada o no.</li>
 *   <li>{@code SALDOS}: debe dinero — lo pagado no cubre el importe + la multa de alguna renta.</li>
 * </ul>
 */
public enum FiltroDeClientes {
	PENDIENTES,
	VENCIDAS,
	MULTAS,
	SALDOS
}
