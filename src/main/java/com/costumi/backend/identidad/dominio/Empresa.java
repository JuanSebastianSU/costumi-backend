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
	// Datos de la solicitud de tienda (marketplace): opcionales para el auto-registro clásico.
	private final String ubicacion;
	private final String contacto;
	private final UUID solicitanteId; // usuario CLIENTE que pidió abrir su tienda (si aplica)

	private Empresa(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro,
			String ubicacion, String contacto, UUID solicitanteId) {
		this.id = Objects.requireNonNull(id, "id");
		this.nombre = exigirNombre(nombre);
		this.estado = Objects.requireNonNull(estado, "estado");
		this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro");
		this.ubicacion = normalizar(ubicacion);
		this.contacto = normalizar(contacto);
		this.solicitanteId = solicitanteId;
	}

	/** Auto-registro clásico (RF-15.2): una empresa nueva nace en estado PENDIENTE, sin datos de solicitud. */
	public static Empresa registrar(String nombre) {
		return registrar(nombre, null, null, null);
	}

	/**
	 * Solicitud de tienda del marketplace: nace PENDIENTE con la ubicación/contacto que cargó el
	 * cliente y el id del cliente solicitante (para que el SuperAdmin sepa a quién promover a Dueño).
	 */
	public static Empresa registrar(String nombre, String ubicacion, String contacto, UUID solicitanteId) {
		return new Empresa(UUID.randomUUID(), nombre, EstadoEmpresa.PENDIENTE, Instant.now(),
				ubicacion, contacto, solicitanteId);
	}

	/** Reconstruye una Empresa desde persistencia (usado por el adaptador de salida). */
	public static Empresa rehidratar(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro,
			String ubicacion, String contacto, UUID solicitanteId) {
		return new Empresa(id, nombre, estado, fechaRegistro, ubicacion, contacto, solicitanteId);
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

	/** Campos opcionales: se guardan recortados o null si vienen vacíos. */
	private static String normalizar(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.trim();
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

	/** Ubicación de la tienda que cargó el cliente al solicitarla (puede ser null en el registro clásico). */
	public String ubicacion() {
		return ubicacion;
	}

	/** Datos de contacto de la solicitud (puede ser null). */
	public String contacto() {
		return contacto;
	}

	/** Id del usuario CLIENTE que solicitó abrir la tienda (null si no vino de una solicitud del marketplace). */
	public UUID solicitanteId() {
		return solicitanteId;
	}
}
