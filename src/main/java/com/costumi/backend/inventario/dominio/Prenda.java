package com.costumi.backend.inventario.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Prenda: ítem concreto de la biblioteca de inventario (RF-2, capa 2). Pertenece a una Empresa
 * (tenant) y a una Categoría, y define su tipo (renta/venta/ambos) y precios (RF-2.1, RF-2.10).
 *
 * <p>Reglas de precio: si el artículo se renta, exige precio de renta; si se vende, precio de venta.
 */
public class Prenda {

	private final UUID id;
	private final UUID empresaId;
	private final UUID categoriaId;
	private String nombre;
	private final TipoArticulo tipoArticulo;
	private BigDecimal precioRenta;
	private BigDecimal precioVenta;
	private boolean archivada;

	private Prenda(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId");
		this.tipoArticulo = Objects.requireNonNull(tipoArticulo, "tipoArticulo");
		this.nombre = exigirNombre(nombre);
		this.precioRenta = validarPrecio(precioRenta, tipoArticulo.incluyeRenta(), "renta");
		this.precioVenta = validarPrecio(precioVenta, tipoArticulo.incluyeVenta(), "venta");
		this.archivada = archivada;
	}

	public static Prenda crear(UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta) {
		return new Prenda(UUID.randomUUID(), empresaId, categoriaId, nombre, tipoArticulo, precioRenta, precioVenta, false);
	}

	public static Prenda rehidratar(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, boolean archivada) {
		return new Prenda(id, empresaId, categoriaId, nombre, tipoArticulo, precioRenta, precioVenta, archivada);
	}

	public void archivar() {
		this.archivada = true;
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre de la prenda es obligatorio");
		}
		return nombre.trim();
	}

	private static BigDecimal validarPrecio(BigDecimal precio, boolean requerido, String concepto) {
		if (requerido) {
			if (precio == null || precio.signum() <= 0) {
				throw new IllegalArgumentException("El precio de " + concepto + " es obligatorio y debe ser mayor a 0");
			}
		} else if (precio != null && precio.signum() < 0) {
			throw new IllegalArgumentException("El precio de " + concepto + " no puede ser negativo");
		}
		return precio;
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID categoriaId() {
		return categoriaId;
	}

	public String nombre() {
		return nombre;
	}

	public TipoArticulo tipoArticulo() {
		return tipoArticulo;
	}

	public BigDecimal precioRenta() {
		return precioRenta;
	}

	public BigDecimal precioVenta() {
		return precioVenta;
	}

	public boolean archivada() {
		return archivada;
	}
}
