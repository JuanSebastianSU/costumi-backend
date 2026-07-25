package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un disfraz que un usuario del marketplace guardó como favorito ("Mis guardados", C4). Se guarda por el
 * usuario (no por tienda: cruza tiendas, como su historial) con un <b>snapshot</b> de nombre/foto/precios,
 * para pintar la lista sin resolver el disfraz en cada dispositivo. La clave lógica es (usuario, disfraz):
 * guardar el mismo disfraz otra vez actualiza el snapshot, no duplica.
 */
public class Favorito {

	private final UUID id;
	private final UUID usuarioId;
	private final UUID disfrazId;
	private final UUID empresaId;
	private final String nombre;
	private final String fotoUrl;
	private final BigDecimal precioRenta;
	private final BigDecimal precioVenta;
	private final Instant guardadoEn;

	private Favorito(UUID id, UUID usuarioId, UUID disfrazId, UUID empresaId, String nombre, String fotoUrl,
			BigDecimal precioRenta, BigDecimal precioVenta, Instant guardadoEn) {
		this.id = Objects.requireNonNull(id, "id");
		this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
		this.disfrazId = Objects.requireNonNull(disfrazId, "disfrazId");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigir(nombre);
		this.fotoUrl = normalizar(fotoUrl);
		this.precioRenta = precioRenta;
		this.precioVenta = precioVenta;
		this.guardadoEn = Objects.requireNonNull(guardadoEn, "guardadoEn");
	}

	/** Guarda un disfraz como favorito del usuario (con la foto/precios que vio el cliente). */
	public static Favorito guardar(UUID usuarioId, UUID disfrazId, UUID empresaId, String nombre, String fotoUrl,
			BigDecimal precioRenta, BigDecimal precioVenta) {
		return new Favorito(UUID.randomUUID(), usuarioId, disfrazId, empresaId, nombre, fotoUrl, precioRenta,
				precioVenta, Instant.now());
	}

	/** Reconstruye un favorito desde persistencia. */
	public static Favorito rehidratar(UUID id, UUID usuarioId, UUID disfrazId, UUID empresaId, String nombre,
			String fotoUrl, BigDecimal precioRenta, BigDecimal precioVenta, Instant guardadoEn) {
		return new Favorito(id, usuarioId, disfrazId, empresaId, nombre, fotoUrl, precioRenta, precioVenta, guardadoEn);
	}

	/** Reusa el id (y la fecha) de un favorito ya existente al re-guardar: actualiza el snapshot sin duplicar. */
	public Favorito conIdentidadDe(Favorito existente) {
		return new Favorito(existente.id, usuarioId, disfrazId, empresaId, nombre, fotoUrl, precioRenta, precioVenta,
				existente.guardadoEn);
	}

	private static String exigir(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre del disfraz es obligatorio");
		}
		return nombre.trim();
	}

	private static String normalizar(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID usuarioId() {
		return usuarioId;
	}

	public UUID disfrazId() {
		return disfrazId;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public String nombre() {
		return nombre;
	}

	public String fotoUrl() {
		return fotoUrl;
	}

	public BigDecimal precioRenta() {
		return precioRenta;
	}

	public BigDecimal precioVenta() {
		return precioVenta;
	}

	public Instant guardadoEn() {
		return guardadoEn;
	}
}
