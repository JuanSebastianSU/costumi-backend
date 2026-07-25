package com.costumi.backend.marketplace.dominio;

import java.util.Objects;
import java.util.UUID;

/** Modelo de lectura de una empresa visible en el marketplace del cliente (RF-18.1). */
public class EmpresaEnVitrina {

	private final UUID id;
	private final String nombre;
	private final String logoUrl;
	private final String portadaUrl;
	private final String ciudad;
	private final String descripcion;
	private final long disfracesCount;

	private EmpresaEnVitrina(UUID id, String nombre, String logoUrl, String portadaUrl, String ciudad,
			String descripcion, long disfracesCount) {
		this.id = Objects.requireNonNull(id, "id");
		this.nombre = Objects.requireNonNull(nombre, "nombre");
		this.logoUrl = logoUrl;
		this.portadaUrl = portadaUrl;
		this.ciudad = ciudad;
		this.descripcion = descripcion;
		this.disfracesCount = disfracesCount;
	}

	public static EmpresaEnVitrina de(UUID id, String nombre, String logoUrl, String portadaUrl, String ciudad,
			String descripcion, long disfracesCount) {
		return new EmpresaEnVitrina(id, nombre, logoUrl, portadaUrl, ciudad, descripcion, disfracesCount);
	}

	public UUID id() {
		return id;
	}

	public String nombre() {
		return nombre;
	}

	public String logoUrl() {
		return logoUrl;
	}

	public String portadaUrl() {
		return portadaUrl;
	}

	public String ciudad() {
		return ciudad;
	}

	public String descripcion() {
		return descripcion;
	}

	public long disfracesCount() {
		return disfracesCount;
	}
}
