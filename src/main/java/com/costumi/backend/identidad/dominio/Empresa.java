package com.costumi.backend.identidad.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Empresa (tenant): el negocio en el nivel superior bajo la Plataforma (RF-15).
 *
 * <p>Agregado de dominio puro: sin Spring, sin JPA, sin web. Encapsula la máquina de
 * estados de aprobación (ver {@link EstadoEmpresa}).
 */
public class Empresa {

	private final UUID id;
	private String nombre;
	private EstadoEmpresa estado;
	private final Instant fechaRegistro;

	private Empresa(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro) {
		this.id = Objects.requireNonNull(id, "id");
		this.nombre = exigirNombre(nombre);
		this.estado = Objects.requireNonNull(estado, "estado");
		this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro");
	}

	/** Auto-registro (RF-15.2): una empresa nueva nace en estado PENDIENTE. */
	public static Empresa registrar(String nombre) {
		return new Empresa(UUID.randomUUID(), nombre, EstadoEmpresa.PENDIENTE, Instant.now());
	}

	/** Reconstruye una Empresa desde persistencia (usado por el adaptador de salida). */
	public static Empresa rehidratar(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro) {
		return new Empresa(id, nombre, estado, fechaRegistro);
	}

	/** Aprueba una empresa PENDIENTE (SuperAdmin, RF-15.3). */
	public void aprobar() {
		transicionarA(EstadoEmpresa.ACTIVA);
	}

	/** Rechaza una empresa PENDIENTE (SuperAdmin, RF-15.3). */
	public void rechazar() {
		transicionarA(EstadoEmpresa.RECHAZADA);
	}

	/** Suspende una empresa ACTIVA (SuperAdmin, RF-15.3). */
	public void suspender() {
		transicionarA(EstadoEmpresa.SUSPENDIDA);
	}

	/** Reactiva una empresa SUSPENDIDA. */
	public void reactivar() {
		transicionarA(EstadoEmpresa.ACTIVA);
	}

	/**
	 * ¿La solicitud de alta está vencida? (RF-15.4): sigue PENDIENTE y ya pasó el plazo
	 * de resolución desde su registro. La plataforma debía responder dentro de ese plazo.
	 */
	public boolean solicitudVencida(Duration plazoResolucion, Instant ahora) {
		return estado == EstadoEmpresa.PENDIENTE && ahora.isAfter(fechaRegistro.plus(plazoResolucion));
	}

	private void transicionarA(EstadoEmpresa destino) {
		if (!estado.puedeTransicionarA(destino)) {
			throw new TransicionDeEstadoInvalida(estado, destino);
		}
		this.estado = destino;
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre de la empresa es obligatorio");
		}
		return nombre.trim();
	}

	public UUID id() {
		return id;
	}

	public String nombre() {
		return nombre;
	}

	public EstadoEmpresa estado() {
		return estado;
	}

	public Instant fechaRegistro() {
		return fechaRegistro;
	}
}
