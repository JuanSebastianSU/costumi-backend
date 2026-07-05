package com.costumi.backend.disfraces.dominio;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Disfraz: el producto que se renta/vende (RF-2.3, Capa 3). Tiene un {@link ModoDeDisfraz}:
 * <ul>
 *   <li>{@code UNIDAD_FIJA}: indivisible; su stock es el de una prenda concreta ({@code prendaFijaId}).</li>
 *   <li>{@code POR_PARTES}: una lista de <b>1..8 slots</b>; puede mezclar slots fijos y personalizables.</li>
 * </ul>
 *
 * <p><b>Disponibilidad derivada (RF-2.4):</b> no es un contador propio; se <b>calcula</b>. Un disfraz por
 * partes está disponible si <b>cada slot obligatorio</b> tiene stock (prenda fija con stock, o pool con
 * stock); los slots opcionales no bloquean. Un disfraz de unidad fija está disponible si su prenda lo está.
 */
public final class Disfraz {

	public static final int MAX_SLOTS = 8;

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private final ModoDeDisfraz modo;
	private final UUID prendaFijaId;
	private final List<Slot> slots;

	private Disfraz(UUID id, UUID empresaId, String nombre, ModoDeDisfraz modo, UUID prendaFijaId, List<Slot> slots) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.modo = Objects.requireNonNull(modo, "modo");
		this.prendaFijaId = prendaFijaId;
		this.slots = List.copyOf(slots == null ? List.of() : slots);
		validar();
	}

	public static Disfraz unidadFija(UUID empresaId, String nombre, UUID prendaFijaId) {
		return new Disfraz(UUID.randomUUID(), empresaId, nombre, ModoDeDisfraz.UNIDAD_FIJA,
				Objects.requireNonNull(prendaFijaId, "prendaFijaId"), List.of());
	}

	public static Disfraz porPartes(UUID empresaId, String nombre, List<Slot> slots) {
		return new Disfraz(UUID.randomUUID(), empresaId, nombre, ModoDeDisfraz.POR_PARTES, null, slots);
	}

	public static Disfraz rehidratar(UUID id, UUID empresaId, String nombre, ModoDeDisfraz modo, UUID prendaFijaId,
			List<Slot> slots) {
		return new Disfraz(id, empresaId, nombre, modo, prendaFijaId, slots);
	}

	/** Disponibilidad derivada (RF-2.4): se calcula a partir de los slots / la prenda fija. */
	public boolean estaDisponible(ConsultaDeStockDePool consulta) {
		Objects.requireNonNull(consulta, "consulta");
		return switch (modo) {
			case UNIDAD_FIJA -> consulta.prendaTieneStock(prendaFijaId);
			case POR_PARTES -> slots.stream().filter(Slot::esObligatorio).allMatch(slot -> slot.puedeCubrirse(consulta));
		};
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	private void validar() {
		if (modo == ModoDeDisfraz.UNIDAD_FIJA) {
			if (prendaFijaId == null) {
				throw new IllegalArgumentException("Un disfraz de unidad fija requiere una prenda");
			}
			if (!slots.isEmpty()) {
				throw new IllegalArgumentException("Un disfraz de unidad fija no lleva slots");
			}
		} else {
			if (prendaFijaId != null) {
				throw new IllegalArgumentException("Un disfraz por partes no lleva prenda fija");
			}
			if (slots.isEmpty() || slots.size() > MAX_SLOTS) {
				throw new IllegalArgumentException("Un disfraz por partes tiene entre 1 y " + MAX_SLOTS + " slots");
			}
		}
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

	public ModoDeDisfraz modo() {
		return modo;
	}

	public UUID prendaFijaId() {
		return prendaFijaId;
	}

	public List<Slot> slots() {
		return slots;
	}
}
