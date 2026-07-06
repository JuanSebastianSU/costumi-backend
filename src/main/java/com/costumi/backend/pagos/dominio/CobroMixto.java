package com.costumi.backend.pagos.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cálculo de un cobro <b>mixto</b> (RF-6.7): un mismo cobro repartido en varias porciones con
 * métodos distintos (p. ej. parte en efectivo, parte con tarjeta). Para el efectivo se registra el
 * <b>monto recibido</b> y se calcula el <b>vuelto</b>. Es dominio puro: no sabe de BD ni de Spring.
 */
public final class CobroMixto {

	private final List<PorcionDePago> porciones;
	private final BigDecimal efectivoRecibido;

	/**
	 * @param porciones       al menos una; la suma es el total del cobro.
	 * @param efectivoRecibido efectivo entregado por el cliente (nulo si no paga efectivo); debe cubrir
	 *                         al menos la parte en efectivo, o se rechaza por insuficiente.
	 */
	public CobroMixto(List<PorcionDePago> porciones, BigDecimal efectivoRecibido) {
		if (porciones == null || porciones.isEmpty()) {
			throw new IllegalArgumentException("Un cobro necesita al menos una porción");
		}
		this.porciones = List.copyOf(porciones);
		BigDecimal esperadoEnEfectivo = efectivoEsperado();
		if (efectivoRecibido != null) {
			if (efectivoRecibido.compareTo(esperadoEnEfectivo) < 0) {
				throw new IllegalArgumentException(
						"El efectivo recibido (" + efectivoRecibido + ") no cubre la parte en efectivo ("
								+ esperadoEnEfectivo + ")");
			}
		}
		this.efectivoRecibido = efectivoRecibido;
	}

	/** Total del cobro = suma de todas las porciones. */
	public BigDecimal total() {
		return porciones.stream().map(PorcionDePago::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** Parte del total que se paga en efectivo (lo que debe entrar a la caja física, RF-6.7). */
	public BigDecimal efectivoEsperado() {
		return porciones.stream().filter(p -> p.metodo() == MetodoPago.EFECTIVO).map(PorcionDePago::monto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** Vuelto a devolver = efectivo recibido − parte en efectivo (0 si no se entregó efectivo). */
	public BigDecimal vuelto() {
		if (efectivoRecibido == null) {
			return BigDecimal.ZERO;
		}
		return efectivoRecibido.subtract(efectivoEsperado());
	}

	public List<PorcionDePago> porciones() {
		return porciones;
	}
}
