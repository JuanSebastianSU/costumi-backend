package com.costumi.backend.devoluciones.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Devolución de una renta (RF-5): el checklist de piezas (RF-5.1) y la liquidación del depósito
 * (RF-5.3): garantía − daños − recargos = remanente a devolver.
 */
public class Devolucion {

	private final UUID id;
	private final UUID empresaId;
	private final UUID rentaId;
	private final BigDecimal deposito;
	private final BigDecimal cargoPorDanos;
	private final BigDecimal cargoPorRetraso;
	private final BigDecimal remanente;
	private final List<PiezaRevisada> piezas;

	private Devolucion(UUID id, UUID empresaId, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
			BigDecimal cargoPorRetraso, BigDecimal remanente, List<PiezaRevisada> piezas) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.rentaId = Objects.requireNonNull(rentaId, "rentaId");
		this.deposito = deposito;
		this.cargoPorDanos = cargoPorDanos;
		this.cargoPorRetraso = cargoPorRetraso;
		this.remanente = remanente;
		this.piezas = new ArrayList<>(piezas);
	}

	public static Devolucion crear(UUID empresaId, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
			BigDecimal cargoPorRetraso, List<PiezaRevisada> piezas) {
		BigDecimal dep = noNegativo(deposito, "depósito");
		BigDecimal danos = noNegativo(cargoPorDanos, "cargo por daños");
		BigDecimal retraso = noNegativo(cargoPorRetraso, "cargo por retraso");
		// Remanente a devolver = garantía − daños − recargos, nunca por debajo de 0 (RF-5.3).
		BigDecimal remanente = dep.subtract(danos).subtract(retraso).max(BigDecimal.ZERO);
		return new Devolucion(UUID.randomUUID(), empresaId, rentaId, dep, danos, retraso, remanente,
				List.copyOf(piezas));
	}

	/**
	 * Multa automática (RF-5.2): lo que el cliente debe <b>por encima</b> del depósito cuando los cargos
	 * (daños + retraso) lo superan. Si el depósito los cubre, la multa es 0.
	 */
	public BigDecimal multa() {
		return cargoPorDanos.add(cargoPorRetraso).subtract(deposito).max(BigDecimal.ZERO);
	}

	public static Devolucion rehidratar(UUID id, UUID empresaId, UUID rentaId, BigDecimal deposito,
			BigDecimal cargoPorDanos, BigDecimal cargoPorRetraso, BigDecimal remanente, List<PiezaRevisada> piezas) {
		return new Devolucion(id, empresaId, rentaId, deposito, cargoPorDanos, cargoPorRetraso, remanente, piezas);
	}

	private static BigDecimal noNegativo(BigDecimal valor, String concepto) {
		BigDecimal v = (valor == null) ? BigDecimal.ZERO : valor;
		if (v.signum() < 0) {
			throw new IllegalArgumentException("El " + concepto + " no puede ser negativo");
		}
		return v;
	}

	public List<PiezaRevisada> piezas() {
		return List.copyOf(piezas);
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID rentaId() {
		return rentaId;
	}

	public BigDecimal deposito() {
		return deposito;
	}

	public BigDecimal cargoPorDanos() {
		return cargoPorDanos;
	}

	public BigDecimal cargoPorRetraso() {
		return cargoPorRetraso;
	}

	public BigDecimal remanente() {
		return remanente;
	}
}
