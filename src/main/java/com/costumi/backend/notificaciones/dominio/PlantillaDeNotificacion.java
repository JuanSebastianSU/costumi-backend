package com.costumi.backend.notificaciones.dominio;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Plantilla de mensaje automático de una empresa para un {@link TipoDeEvento} (RF-11). Agregado de
 * dominio puro: sin Spring/JPA. El texto lleva variables entre llaves que se reemplazan al enviar.
 */
public class PlantillaDeNotificacion {

	private final UUID id;
	private final UUID empresaId;
	private final TipoDeEvento tipo;
	private String texto;
	private boolean activa;

	private PlantillaDeNotificacion(UUID id, UUID empresaId, TipoDeEvento tipo, String texto, boolean activa) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.tipo = Objects.requireNonNull(tipo, "tipo");
		this.texto = exigirTexto(texto);
		this.activa = activa;
	}

	/** Plantilla por defecto de la empresa para un tipo (aún no personalizada / no persistida). */
	public static PlantillaDeNotificacion porDefecto(UUID empresaId, TipoDeEvento tipo) {
		return new PlantillaDeNotificacion(UUID.randomUUID(), empresaId, tipo, tipo.textoPorDefecto(),
				tipo.activaPorDefecto());
	}

	/** Reconstruye desde persistencia. */
	public static PlantillaDeNotificacion rehidratar(UUID id, UUID empresaId, TipoDeEvento tipo, String texto,
			boolean activa) {
		return new PlantillaDeNotificacion(id, empresaId, tipo, texto, activa);
	}

	/** Edita el texto y el switch on/off de la automatización. */
	public void editar(String texto, boolean activa) {
		this.texto = exigirTexto(texto);
		this.activa = activa;
	}

	/**
	 * Devuelve el texto con las variables reemplazadas por los valores provistos (clave sin llaves,
	 * p. ej. {@code "cliente"}). Las variables no provistas se dejan como estén.
	 */
	public String render(Map<String, String> variables) {
		String resultado = texto;
		for (Map.Entry<String, String> var : variables.entrySet()) {
			resultado = resultado.replace("{" + var.getKey() + "}", var.getValue() == null ? "" : var.getValue());
		}
		return resultado;
	}

	private static String exigirTexto(String texto) {
		if (texto == null || texto.isBlank()) {
			throw new IllegalArgumentException("El texto de la plantilla es obligatorio");
		}
		return texto.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public TipoDeEvento tipo() {
		return tipo;
	}

	public String texto() {
		return texto;
	}

	public boolean activa() {
		return activa;
	}
}
