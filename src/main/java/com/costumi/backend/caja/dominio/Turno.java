package com.costumi.backend.caja.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Turno de caja (RF-6.3/6.10): se abre con un fondo inicial (efectivo), acumula movimientos por método
 * de pago y se cierra con el <b>corte</b> (total esperado por método) y el <b>cuadre</b> de efectivo
 * (contado físico vs esperado). Agregado de dominio; el dinero se maneja en {@link BigDecimal}.
 */
public class Turno {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID empleadoId;
	private final BigDecimal fondoInicial;
	private EstadoTurno estado;
	private BigDecimal efectivoContado;
	private final List<MovimientoDeCaja> movimientos;

	private Turno(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, BigDecimal fondoInicial,
			EstadoTurno estado, BigDecimal efectivoContado, List<MovimientoDeCaja> movimientos) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.empleadoId = Objects.requireNonNull(empleadoId, "empleadoId");
		if (fondoInicial == null || fondoInicial.signum() < 0) {
			throw new IllegalArgumentException("El fondo inicial no puede ser negativo");
		}
		this.fondoInicial = fondoInicial;
		this.estado = Objects.requireNonNull(estado, "estado");
		this.efectivoContado = efectivoContado;
		this.movimientos = new ArrayList<>(movimientos);
	}

	public static Turno abrir(UUID empresaId, UUID sucursalId, UUID empleadoId, BigDecimal fondoInicial) {
		return new Turno(UUID.randomUUID(), empresaId, sucursalId, empleadoId, fondoInicial, EstadoTurno.ABIERTO,
				null, new ArrayList<>());
	}

	public static Turno rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, BigDecimal fondoInicial,
			EstadoTurno estado, BigDecimal efectivoContado, List<MovimientoDeCaja> movimientos) {
		return new Turno(id, empresaId, sucursalId, empleadoId, fondoInicial, estado, efectivoContado, movimientos);
	}

	/** Registra un movimiento; solo si el turno sigue abierto (RF-6.3). */
	public void registrar(MovimientoDeCaja movimiento) {
		exigirAbierto();
		movimientos.add(Objects.requireNonNull(movimiento, "movimiento"));
	}

	/** Cierra el turno con el efectivo físico contado (arqueo, RF-6.10). */
	public void cerrar(BigDecimal efectivoContado) {
		exigirAbierto();
		if (efectivoContado == null || efectivoContado.signum() < 0) {
			throw new IllegalArgumentException("El efectivo contado no puede ser negativo");
		}
		this.efectivoContado = efectivoContado;
		this.estado = EstadoTurno.CERRADO;
	}

	/** Corte: total esperado en la caja por método (el efectivo incluye el fondo inicial). */
	public BigDecimal totalPorMetodo(MetodoDePago metodo) {
		BigDecimal base = (metodo == MetodoDePago.EFECTIVO) ? fondoInicial : BigDecimal.ZERO;
		return movimientos.stream()
				.filter(movimiento -> movimiento.metodo() == metodo)
				.map(MovimientoDeCaja::montoConSigno)
				.reduce(base, BigDecimal::add);
	}

	/** Efectivo esperado en caja al cierre (fondo + ingresos − egresos en efectivo). */
	public BigDecimal efectivoEsperado() {
		return totalPorMetodo(MetodoDePago.EFECTIVO);
	}

	/** Diferencia del cuadre: contado − esperado (positivo = sobra, negativo = falta). Solo si está cerrado. */
	public BigDecimal diferenciaDeEfectivo() {
		if (estado != EstadoTurno.CERRADO) {
			throw new IllegalStateException("El turno aún no está cerrado");
		}
		return efectivoContado.subtract(efectivoEsperado());
	}

	private void exigirAbierto() {
		if (estado != EstadoTurno.ABIERTO) {
			throw new TurnoNoAbierto();
		}
	}

	public List<MovimientoDeCaja> movimientos() {
		return List.copyOf(movimientos);
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID sucursalId() {
		return sucursalId;
	}

	public UUID empleadoId() {
		return empleadoId;
	}

	public BigDecimal fondoInicial() {
		return fondoInicial;
	}

	public EstadoTurno estado() {
		return estado;
	}

	public BigDecimal efectivoContado() {
		return efectivoContado;
	}
}
