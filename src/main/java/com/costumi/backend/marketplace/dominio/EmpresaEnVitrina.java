package com.costumi.backend.marketplace.dominio;

import java.util.Objects;
import java.util.UUID;

/** Modelo de lectura de una empresa visible en el marketplace del cliente (RF-18.1). */
public class EmpresaEnVitrina {

	private final UUID id;
	private final String nombre;
	private final String logoUrl;
	private final String ciudad;
	private final String descripcion;

	private EmpresaEnVitrina(UUID id, String nombre, String logoUrl, String ciudad, String descripcion) {
		this.id = Objects.requireNonNull(id, "id");
		this.nombre = Objects.requireNonNull(nombre, "nombre");
		this.logoUrl = logoUrl;
		this.ciudad = ciudad;
		this.descripcion = descripcion;
	}

	public static EmpresaEnVitrina de(UUID id, String nombre, String logoUrl, String ciudad, String descripcion) {
		return new EmpresaEnVitrina(id, nombre, logoUrl, ciudad, descripcion);
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

	public String ciudad() {
		return ciudad;
	}

	public String descripcion() {
		return descripcion;
	}
}
