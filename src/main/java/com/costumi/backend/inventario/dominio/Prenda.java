package com.costumi.backend.inventario.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Prenda: ítem concreto de la biblioteca de inventario (RF-2, capa 2). Pertenece a una Empresa
 * (tenant) y a una Categoría, define su tipo (renta/venta/ambos) y precios (RF-2.1, RF-2.10) y
 * <b>lleva sus valores de etiqueta</b> que la clasifican (RF-2.7, {@link EtiquetasDePrenda}).
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
	private BigDecimal costoAdquisicion;
	private BigDecimal depositoSugerido;
	private BigDecimal valorReposicion;
	private BigDecimal valorDano;
	private EtiquetasDePrenda etiquetas;
	private boolean archivada;
	private String fotoUrl; // URL de la foto (RF-2.9); null si aún no tiene

	private Prenda(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal costoAdquisicion, BigDecimal depositoSugerido,
			BigDecimal valorReposicion, BigDecimal valorDano, EtiquetasDePrenda etiquetas, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId");
		this.tipoArticulo = Objects.requireNonNull(tipoArticulo, "tipoArticulo");
		this.nombre = exigirNombre(nombre);
		this.precioRenta = validarPrecio(precioRenta, tipoArticulo.incluyeRenta(), "renta");
		this.precioVenta = validarPrecio(precioVenta, tipoArticulo.incluyeVenta(), "venta");
		this.costoAdquisicion = validarNoNegativo(costoAdquisicion, "costo de adquisición");
		this.depositoSugerido = validarNoNegativo(depositoSugerido, "depósito sugerido");
		this.valorReposicion = validarNoNegativo(valorReposicion, "valor de reposición");
		this.valorDano = validarNoNegativo(valorDano, "valor de daño");
		this.etiquetas = Objects.requireNonNull(etiquetas, "etiquetas");
		this.archivada = archivada;
	}

	public static Prenda crear(UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta) {
		return crear(empresaId, categoriaId, nombre, tipoArticulo, precioRenta, precioVenta,
				EtiquetasDePrenda.ninguna(), null, null, null, null);
	}

	public static Prenda crear(UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, EtiquetasDePrenda etiquetas) {
		return crear(empresaId, categoriaId, nombre, tipoArticulo, precioRenta, precioVenta, etiquetas, null, null,
				null, null);
	}

	public static Prenda crear(UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, EtiquetasDePrenda etiquetas, BigDecimal costoAdquisicion,
			BigDecimal depositoSugerido, BigDecimal valorReposicion, BigDecimal valorDano) {
		return new Prenda(UUID.randomUUID(), empresaId, categoriaId, nombre, tipoArticulo, precioRenta, precioVenta,
				costoAdquisicion, depositoSugerido, valorReposicion, valorDano, etiquetas, false);
	}

	public static Prenda rehidratar(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal costoAdquisicion, BigDecimal depositoSugerido,
			BigDecimal valorReposicion, BigDecimal valorDano, EtiquetasDePrenda etiquetas, boolean archivada,
			String fotoUrl) {
		Prenda prenda = new Prenda(id, empresaId, categoriaId, nombre, tipoArticulo, precioRenta, precioVenta,
				costoAdquisicion, depositoSugerido, valorReposicion, valorDano, etiquetas, archivada);
		prenda.fotoUrl = fotoUrl;
		return prenda;
	}

	/** Asigna/actualiza la URL de la foto de la prenda (RF-2.9). */
	public void asignarFoto(String url) {
		this.fotoUrl = url;
	}

	public String fotoUrl() {
		return fotoUrl;
	}

	/** Reemplaza las etiquetas de clasificación de la prenda (RF-2.7, Capa 2). */
	public void reetiquetar(EtiquetasDePrenda nuevas) {
		this.etiquetas = Objects.requireNonNull(nuevas, "etiquetas");
	}

	public void archivar() {
		this.archivada = true;
	}

	/** Reactiva una prenda archivada: vuelve a estar disponible para operar. */
	public void activar() {
		this.archivada = false;
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	/**
	 * Edita los datos de la prenda (RF-2.10): nombre, precios y valores, revalidando contra su
	 * {@code tipoArticulo} (que no cambia). Las etiquetas se reemplazan aparte con {@link #reetiquetar}.
	 */
	public void editar(String nombre, BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal costoAdquisicion,
			BigDecimal depositoSugerido, BigDecimal valorReposicion, BigDecimal valorDano) {
		this.nombre = exigirNombre(nombre);
		this.precioRenta = validarPrecio(precioRenta, tipoArticulo.incluyeRenta(), "renta");
		this.precioVenta = validarPrecio(precioVenta, tipoArticulo.incluyeVenta(), "venta");
		this.costoAdquisicion = validarNoNegativo(costoAdquisicion, "costo de adquisición");
		this.depositoSugerido = validarNoNegativo(depositoSugerido, "depósito sugerido");
		this.valorReposicion = validarNoNegativo(valorReposicion, "valor de reposición");
		this.valorDano = validarNoNegativo(valorDano, "valor de daño");
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

	private static BigDecimal validarNoNegativo(BigDecimal valor, String concepto) {
		if (valor != null && valor.signum() < 0) {
			throw new IllegalArgumentException("El " + concepto + " no puede ser negativo");
		}
		return valor;
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

	/** Costo de adquisición (RF-2.10): sin costo no hay margen en reportes. Opcional. */
	public BigDecimal costoAdquisicion() {
		return costoAdquisicion;
	}

	/** Depósito/garantía sugerido para la renta (RF-2.10). Opcional. */
	public BigDecimal depositoSugerido() {
		return depositoSugerido;
	}

	/** Valor a cobrar si la prenda se pierde o no se devuelve (reposición). Opcional (RF-5.2/5.6). */
	public BigDecimal valorReposicion() {
		return valorReposicion;
	}

	/** Valor sugerido a cobrar si la prenda vuelve dañada (RF-5.2/5.6). Opcional. */
	public BigDecimal valorDano() {
		return valorDano;
	}

	public EtiquetasDePrenda etiquetas() {
		return etiquetas;
	}

	public boolean archivada() {
		return archivada;
	}
}
