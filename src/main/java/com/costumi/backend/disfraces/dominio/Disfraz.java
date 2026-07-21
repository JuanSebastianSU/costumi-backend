package com.costumi.backend.disfraces.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Disfraz: el producto que se renta/vende (RF-2.3, Capa 3). <b>Siempre es un conjunto de prendas</b>:
 * una lista de <b>1..8 slots</b> (secciones). Lo que cambia es qué partes son personalizables:
 * <ul>
 *   <li>Slot <b>fijo</b>: siempre la misma prenda ({@code prendaFijaId}).</li>
 *   <li>Slot <b>personalizable</b>: el cliente elige del {@link PoolDeSlot} (categoría + etiquetas).</li>
 * </ul>
 * Un disfraz de "una sola pieza" es simplemente un disfraz con un único slot fijo.
 *
 * <p><b>Disponibilidad derivada (RF-2.4):</b> no es un contador propio; se <b>calcula</b>. Está
 * disponible si <b>cada slot obligatorio</b> tiene stock (prenda fija con stock, o pool con stock);
 * los slots opcionales no bloquean.
 *
 * <p>Un disfraz {@code archivado} ({@code activo == false}) deja de ofrecerse en la vitrina y no se
 * puede rentar, pero se conserva para el historial.
 *
 * <p><b>Precio (RF-2.10):</b> por defecto el precio de renta se deriva de la <b>suma de las prendas</b>
 * que lo componen. Opcionalmente el dueño puede fijar un {@code precioRentaGeneral} (por día) que
 * <b>anula</b> esa suma y cobra ese valor por el conjunto. Nulo = se cobra por prendas.
 */
public final class Disfraz {

	public static final int MAX_SLOTS = 8;

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private UUID categoriaId;
	private List<Slot> slots;
	private boolean activo;
	private BigDecimal precioRentaGeneral;
	private String fotoUrl;

	private Disfraz(UUID id, UUID empresaId, String nombre, UUID categoriaId, List<Slot> slots, boolean activo,
			BigDecimal precioRentaGeneral, String fotoUrl) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.categoriaId = categoriaId;
		this.slots = exigirSlots(slots);
		this.activo = activo;
		this.precioRentaGeneral = validarPrecio(precioRentaGeneral);
		this.fotoUrl = fotoUrl;
	}

	public static Disfraz crear(UUID empresaId, String nombre, UUID categoriaId, List<Slot> slots,
			BigDecimal precioRentaGeneral) {
		return new Disfraz(UUID.randomUUID(), empresaId, nombre, categoriaId, slots, true, precioRentaGeneral, null);
	}

	/** Crea un disfraz sin categoría (compatibilidad); se cobra por prendas si no hay precio general. */
	public static Disfraz crear(UUID empresaId, String nombre, List<Slot> slots, BigDecimal precioRentaGeneral) {
		return crear(empresaId, nombre, null, slots, precioRentaGeneral);
	}

	/** Crea un disfraz que se cobra por prendas (sin categoría ni precio general). */
	public static Disfraz crear(UUID empresaId, String nombre, List<Slot> slots) {
		return crear(empresaId, nombre, null, slots, null);
	}

	public static Disfraz rehidratar(UUID id, UUID empresaId, String nombre, UUID categoriaId, List<Slot> slots,
			boolean activo, BigDecimal precioRentaGeneral, String fotoUrl) {
		return new Disfraz(id, empresaId, nombre, categoriaId, slots, activo, precioRentaGeneral, fotoUrl);
	}

	/** Asigna/actualiza la foto del disfraz (la que sube el dueño, RF-2.9), tras subirla al almacén. */
	public void asignarFoto(String url) {
		this.fotoUrl = url;
	}

	/** Disponibilidad derivada (RF-2.4): se calcula a partir de los slots obligatorios. */
	public boolean estaDisponible(ConsultaDeStockDePool consulta) {
		Objects.requireNonNull(consulta, "consulta");
		return slots.stream().filter(Slot::esObligatorio).allMatch(slot -> slot.puedeCubrirse(consulta));
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	/** Redefine el disfraz completo (nombre + categoría + slots + precio general) al editarlo (RF-2.3). */
	public void redefinir(String nuevoNombre, UUID nuevaCategoriaId, List<Slot> nuevosSlots,
			BigDecimal precioRentaGeneral) {
		this.nombre = exigirNombre(nuevoNombre);
		this.categoriaId = nuevaCategoriaId;
		this.slots = exigirSlots(nuevosSlots);
		this.precioRentaGeneral = validarPrecio(precioRentaGeneral);
	}

	/** ¿El disfraz cobra un precio general (que anula la suma por prendas)? */
	public boolean tienePrecioGeneral() {
		return precioRentaGeneral != null;
	}

	/** Lo retira de la vitrina y del alta de rentas, sin borrarlo (conserva historial). */
	public void archivar() {
		this.activo = false;
	}

	public void activar() {
		this.activo = true;
	}

	private static List<Slot> exigirSlots(List<Slot> slots) {
		List<Slot> copia = List.copyOf(slots == null ? List.of() : slots);
		if (copia.isEmpty() || copia.size() > MAX_SLOTS) {
			throw new IllegalArgumentException("Un disfraz tiene entre 1 y " + MAX_SLOTS + " slots");
		}
		return copia;
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre del disfraz es obligatorio");
		}
		return nombre.trim();
	}

	private static BigDecimal validarPrecio(BigDecimal precio) {
		if (precio != null && precio.signum() < 0) {
			throw new IllegalArgumentException("El precio general del disfraz no puede ser negativo");
		}
		return precio;
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

	/** Categoría del disfraz (RF-2.3), para agruparlos/verlos por categoría; nula en disfraces antiguos. */
	public UUID categoriaId() {
		return categoriaId;
	}

	public List<Slot> slots() {
		return slots;
	}

	public boolean activo() {
		return activo;
	}

	/** Precio de renta por día del conjunto que anula la suma por prendas; nulo = se cobra por prendas. */
	public BigDecimal precioRentaGeneral() {
		return precioRentaGeneral;
	}

	/** Foto del disfraz que subió el dueño (URL pública), o null si aún no tiene. */
	public String fotoUrl() {
		return fotoUrl;
	}
}
