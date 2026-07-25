package com.costumi.backend.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Sucursal: local físico de una Empresa; ancla de inventario/ventas/reservas (RF-15.1, RF-14.1).
 *
 * <p>Toda Sucursal pertenece a una Empresa (tenant): lleva su {@code empresaId}. Agregado de
 * dominio puro, sin framework.
 */
public class Sucursal {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private String direccion;
	private String ubicacionMaps;
	private boolean archivada;
	// Media y ubicación del punto físico (A7/G17): las muestra la vitrina; el cliente elige dónde retira.
	private String descripcion;
	private Double latitud;
	private Double longitud;
	private String fotoUrl;

	private Sucursal(UUID id, UUID empresaId, String nombre, String direccion, String ubicacionMaps,
			boolean archivada, String descripcion, Double latitud, Double longitud, String fotoUrl) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.direccion = normalizar(direccion);
		this.ubicacionMaps = normalizar(ubicacionMaps);
		this.archivada = archivada;
		this.descripcion = normalizar(descripcion);
		this.latitud = validarLatitud(latitud);
		this.longitud = validarLongitud(longitud);
		this.fotoUrl = normalizar(fotoUrl);
	}

	/** Crea una sucursal nueva para una empresa (nace activa). Los datos ricos se cargan luego al editar. */
	public static Sucursal crear(UUID empresaId, String nombre, String direccion, String ubicacionMaps) {
		return new Sucursal(UUID.randomUUID(), empresaId, nombre, direccion, ubicacionMaps, false, null, null, null,
				null);
	}

	/** Reconstruye una Sucursal desde persistencia. */
	public static Sucursal rehidratar(UUID id, UUID empresaId, String nombre, String direccion, String ubicacionMaps,
			boolean archivada, String descripcion, Double latitud, Double longitud, String fotoUrl) {
		return new Sucursal(id, empresaId, nombre, direccion, ubicacionMaps, archivada, descripcion, latitud, longitud,
				fotoUrl);
	}

	/**
	 * Edita los datos de la sucursal: nombre (obligatorio), y dirección/maps/descripción/coordenadas
	 * (opcionales). La foto se cambia por su endpoint aparte.
	 */
	public void editar(String nombre, String direccion, String ubicacionMaps, String descripcion, Double latitud,
			Double longitud) {
		this.nombre = exigirNombre(nombre);
		this.direccion = normalizar(direccion);
		this.ubicacionMaps = normalizar(ubicacionMaps);
		this.descripcion = normalizar(descripcion);
		this.latitud = validarLatitud(latitud);
		this.longitud = validarLongitud(longitud);
	}

	/** Asigna la foto ya subida (URL pública del almacén de imágenes). */
	public void asignarFoto(String url) {
		this.fotoUrl = normalizar(url);
	}

	/** Archiva la sucursal: la retira de la operación sin borrarla (conserva su historial). */
	public void archivar() {
		this.archivada = true;
	}

	/** Reactiva una sucursal archivada: vuelve a estar disponible para operar. */
	public void activar() {
		this.archivada = false;
	}

	public boolean archivada() {
		return archivada;
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre de la sucursal es obligatorio");
		}
		return nombre.trim();
	}

	private static String normalizar(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.trim();
	}

	private static Double validarLatitud(Double latitud) {
		if (latitud != null && (latitud < -90 || latitud > 90)) {
			throw new IllegalArgumentException("La latitud debe estar entre -90 y 90");
		}
		return latitud;
	}

	private static Double validarLongitud(Double longitud) {
		if (longitud != null && (longitud < -180 || longitud > 180)) {
			throw new IllegalArgumentException("La longitud debe estar entre -180 y 180");
		}
		return longitud;
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public String nombre() {
		return nombre;
	}

	public String direccion() {
		return direccion;
	}

	public String ubicacionMaps() {
		return ubicacionMaps;
	}

	/** Descripción del punto físico para la vitrina (puede ser null). */
	public String descripcion() {
		return descripcion;
	}

	/** Latitud del punto en el mapa (puede ser null si no se cargó). */
	public Double latitud() {
		return latitud;
	}

	/** Longitud del punto en el mapa (puede ser null). */
	public Double longitud() {
		return longitud;
	}

	/** URL de la foto de la sucursal (puede ser null). */
	public String fotoUrl() {
		return fotoUrl;
	}
}
