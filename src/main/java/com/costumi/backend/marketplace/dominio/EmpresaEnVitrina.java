package com.costumi.backend.marketplace.dominio;

import java.util.Objects;
import java.util.UUID;

/** Modelo de lectura de una empresa visible en el marketplace del cliente (RF-18.1). */
public class EmpresaEnVitrina {

	private final UUID id;
	private final String nombre;

	private EmpresaEnVitrina(UUID id, String nombre) {
		this.id = Objects.requireNonNull(id, "id");
		this.nombre = Objects.requireNonNull(nombre, "nombre");
	}

	public static EmpresaEnVitrina de(UUID id, String nombre) {
		return new EmpresaEnVitrina(id, nombre);
	}

	public UUID id() {
		return id;
	}

	public String nombre() {
		return nombre;
	}
}
