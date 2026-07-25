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
	private String ubicacion;
	private String contacto;
	private final UUID solicitanteId; // usuario CLIENTE que pidió abrir su tienda (si aplica)
	// Identidad/marca de la tienda (A7): la edita el Dueño; la vitrina del marketplace las muestra.
	private String descripcion;
	private String ciudad;
	private String logoUrl;
	private String portadaUrl;

	private Empresa(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro,
			String ubicacion, String contacto, UUID solicitanteId, String descripcion, String ciudad,
			String logoUrl, String portadaUrl) {
		this.id = Objects.requireNonNull(id, "id");
		this.nombre = exigirNombre(nombre);
		this.estado = Objects.requireNonNull(estado, "estado");
		this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro");
		this.ubicacion = normalizar(ubicacion);
		this.contacto = normalizar(contacto);
		this.solicitanteId = solicitanteId;
		this.descripcion = normalizar(descripcion);
		this.ciudad = normalizar(ciudad);
		this.logoUrl = normalizar(logoUrl);
		this.portadaUrl = normalizar(portadaUrl);
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
				ubicacion, contacto, solicitanteId, null, null, null, null);
	}

	/** Reconstruye una Empresa desde persistencia (usado por el adaptador de salida). */
	public static Empresa rehidratar(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro,
			String ubicacion, String contacto, UUID solicitanteId, String descripcion, String ciudad,
			String logoUrl, String portadaUrl) {
		return new Empresa(id, nombre, estado, fechaRegistro, ubicacion, contacto, solicitanteId, descripcion,
				ciudad, logoUrl, portadaUrl);
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

	/** El Dueño edita la identidad de su tienda (A7): nombre + datos y textos de marca. Campos vacíos → null. */
	public void editarIdentidad(String nombre, String ubicacion, String contacto, String descripcion, String ciudad) {
		this.nombre = exigirNombre(nombre);
		this.ubicacion = normalizar(ubicacion);
		this.contacto = normalizar(contacto);
		this.descripcion = normalizar(descripcion);
		this.ciudad = normalizar(ciudad);
	}

	/** Asigna el logo ya subido (URL pública del almacén de imágenes). */
	public void asignarLogo(String url) {
		this.logoUrl = normalizar(url);
	}

	/** Asigna la portada/banner ya subido (URL pública del almacén de imágenes). */
	public void asignarPortada(String url) {
		this.portadaUrl = normalizar(url);
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

	/** Descripción/bio de la tienda para su vitrina (puede ser null). */
	public String descripcion() {
		return descripcion;
	}

	/** Ciudad de la tienda (para el marketplace/saludo del cliente; puede ser null). */
	public String ciudad() {
		return ciudad;
	}

	/** URL del logo de la tienda (puede ser null). */
	public String logoUrl() {
		return logoUrl;
	}

	/** URL de la portada/banner de la tienda (puede ser null). */
	public String portadaUrl() {
		return portadaUrl;
	}
}
