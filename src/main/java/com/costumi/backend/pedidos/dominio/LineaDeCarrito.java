package com.costumi.backend.pedidos.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Línea de un carrito: una <b>prenda</b> (prendaId) o un <b>disfraz</b> (disfrazId), nunca ambos. La
 * línea de disfraz lleva además la elección de prenda por slot personalizable ({@code selecciones}). En
 * los carritos de RENTA lleva su periodo (retiro/devolución) — cada artículo puede tener fechas propias
 * (RF-18.6); en los de VENTA las fechas son nulas.
 */
public class LineaDeCarrito {

	private final UUID prendaId;
	private final UUID disfrazId;
	private final List<SeleccionDeSlot> selecciones;
	private int cantidad;
	private final LocalDate fechaRetiro;
	private final LocalDate fechaDevolucion;

	private LineaDeCarrito(UUID prendaId, UUID disfrazId, List<SeleccionDeSlot> selecciones, int cantidad,
			LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		if ((prendaId == null) == (disfrazId == null)) {
			throw new IllegalArgumentException("Una línea es de una prenda O de un disfraz (exactamente uno)");
		}
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
		}
		if (fechaRetiro != null && fechaDevolucion != null && fechaDevolucion.isBefore(fechaRetiro)) {
			throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la de retiro");
		}
		this.prendaId = prendaId;
		this.disfrazId = disfrazId;
		this.selecciones = ordenadas(selecciones);
		this.cantidad = cantidad;
		this.fechaRetiro = fechaRetiro;
		this.fechaDevolucion = fechaDevolucion;
	}

	public static LineaDeCarrito de(UUID prendaId, int cantidad) {
		return new LineaDeCarrito(prendaId, null, List.of(), cantidad, null, null);
	}

	public static LineaDeCarrito de(UUID prendaId, int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		return new LineaDeCarrito(prendaId, null, List.of(), cantidad, fechaRetiro, fechaDevolucion);
	}

	public static LineaDeCarrito deDisfraz(UUID disfrazId, List<SeleccionDeSlot> selecciones, int cantidad,
			LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		return new LineaDeCarrito(null, disfrazId, selecciones, cantidad, fechaRetiro, fechaDevolucion);
	}

	/** Selecciones normalizadas (ordenadas por {@code orden}) para que la comparación de clave sea estable. */
	private static List<SeleccionDeSlot> ordenadas(List<SeleccionDeSlot> selecciones) {
		if (selecciones == null || selecciones.isEmpty()) {
			return List.of();
		}
		List<SeleccionDeSlot> copia = new ArrayList<>(selecciones);
		copia.sort(Comparator.comparingInt(SeleccionDeSlot::orden));
		return List.copyOf(copia);
	}

	void incrementar(int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a agregar debe ser mayor a 0");
		}
		this.cantidad += cantidad;
	}

	/** ¿Agrupa con (prenda, periodo)? Solo aplica a líneas de prenda. */
	boolean mismaClave(UUID prendaId, LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		return esPrenda()
				&& this.prendaId.equals(prendaId)
				&& Objects.equals(this.fechaRetiro, fechaRetiro)
				&& Objects.equals(this.fechaDevolucion, fechaDevolucion);
	}

	/** ¿Agrupa con (disfraz, misma elección por slot, periodo)? Solo aplica a líneas de disfraz. */
	boolean mismaClaveDisfraz(UUID disfrazId, List<SeleccionDeSlot> selecciones, LocalDate fechaRetiro,
			LocalDate fechaDevolucion) {
		return esDisfraz()
				&& this.disfrazId.equals(disfrazId)
				&& this.selecciones.equals(ordenadas(selecciones))
				&& Objects.equals(this.fechaRetiro, fechaRetiro)
				&& Objects.equals(this.fechaDevolucion, fechaDevolucion);
	}

	public boolean esPrenda() {
		return prendaId != null;
	}

	public boolean esDisfraz() {
		return disfrazId != null;
	}

	public UUID prendaId() {
		return prendaId;
	}

	public UUID disfrazId() {
		return disfrazId;
	}

	public List<SeleccionDeSlot> selecciones() {
		return selecciones;
	}

	public int cantidad() {
		return cantidad;
	}

	public LocalDate fechaRetiro() {
		return fechaRetiro;
	}

	public LocalDate fechaDevolucion() {
		return fechaDevolucion;
	}
}
