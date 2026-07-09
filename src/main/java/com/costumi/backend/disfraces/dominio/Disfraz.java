package com.costumi.backend.disfraces.dominio;

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
 */
public final class Disfraz {

	public static final int MAX_SLOTS = 8;

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private List<Slot> slots;
	private boolean activo;

	private Disfraz(UUID id, UUID empresaId, String nombre, List<Slot> slots, boolean activo) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.slots = exigirSlots(slots);
		this.activo = activo;
	}

	public static Disfraz crear(UUID empresaId, String nombre, List<Slot> slots) {
		return new Disfraz(UUID.randomUUID(), empresaId, nombre, slots, true);
	}

	public static Disfraz rehidratar(UUID id, UUID empresaId, String nombre, List<Slot> slots, boolean activo) {
		return new Disfraz(id, empresaId, nombre, slots, activo);
	}

	/** Disponibilidad derivada (RF-2.4): se calcula a partir de los slots obligatorios. */
	public boolean estaDisponible(ConsultaDeStockDePool consulta) {
		Objects.requireNonNull(consulta, "consulta");
		return slots.stream().filter(Slot::esObligatorio).allMatch(slot -> slot.puedeCubrirse(consulta));
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	/** Redefine el disfraz completo (nombre + slots) al editarlo (RF-2.3). */
	public void redefinir(String nuevoNombre, List<Slot> nuevosSlots) {
		this.nombre = exigirNombre(nuevoNombre);
		this.slots = exigirSlots(nuevosSlots);
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

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public String nombre() {
		return nombre;
	}

	public List<Slot> slots() {
		return slots;
	}

	public boolean activo() {
		return activo;
	}
}
