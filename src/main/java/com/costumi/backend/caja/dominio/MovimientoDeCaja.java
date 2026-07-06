package com.costumi.backend.caja.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/** Un movimiento de caja: ingreso o egreso, por un método de pago, con su monto (RF-6.3). */
public class MovimientoDeCaja {

	private final TipoMovimiento tipo;
	private final String concepto;
	private final BigDecimal monto;
	private final MetodoDePago metodo;

	private MovimientoDeCaja(TipoMovimiento tipo, String concepto, BigDecimal monto, MetodoDePago metodo) {
		this.tipo = Objects.requireNonNull(tipo, "tipo");
		this.metodo = Objects.requireNonNull(metodo, "metodo");
		if (concepto == null || concepto.isBlank()) {
			throw new IllegalArgumentException("El concepto del movimiento es obligatorio");
		}
		if (monto == null || monto.signum() <= 0) {
			throw new IllegalArgumentException("El monto del movimiento debe ser mayor a 0");
		}
		this.concepto = concepto.trim();
		this.monto = monto;
	}

	public static MovimientoDeCaja de(TipoMovimiento tipo, String concepto, BigDecimal monto, MetodoDePago metodo) {
		return new MovimientoDeCaja(tipo, concepto, monto, metodo);
	}

	/** Monto con signo: los ingresos suman, los egresos restan. */
	public BigDecimal montoConSigno() {
		return tipo == TipoMovimiento.INGRESO ? monto : monto.negate();
	}

	public TipoMovimiento tipo() {
		return tipo;
	}

	public String concepto() {
		return concepto;
	}

	public BigDecimal monto() {
		return monto;
	}

	public MetodoDePago metodo() {
		return metodo;
	}
}
