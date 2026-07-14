package com.costumi.backend.notificaciones.dominio;

/**
 * Categorías de mensaje automático que una empresa puede configurar (RF-11). Cada tipo trae su
 * texto por defecto, con variables entre llaves que se reemplazan al enviar (ver
 * {@link PlantillaDeNotificacion#render}). No todas las variables aplican a todo tipo: el disparador
 * de cada evento sólo provee las que tiene a mano.
 *
 * <p>Variables reconocidas: {@code {cliente}}, {@code {tienda}}, {@code {direccion}}, {@code {maps}},
 * {@code {fecha_devolucion}}, {@code {horas_restantes}}, {@code {monto}}, {@code {articulo}}.
 */
public enum TipoDeEvento {

	MULTA_GENERADA(
			"Hola {cliente}, se registró una multa de {monto} en tu devolución. "
					+ "Por favor acércate a saldarla. ¡Gracias!"),

	DEUDA_SALDADA(
			"¡Listo {cliente}! Ya saldaste tu deuda. Gracias por ponerte al día."),

	RENTA_CONFIRMADA(
			"Hola {cliente}, tu renta quedó confirmada. Recordá devolverla el {fecha_devolucion} "
					+ "en {direccion}. {maps}"),

	RECORDATORIO_DEVOLUCION(
			"Hola {cliente}, te quedan {horas_restantes} para devolver tu renta (vence el "
					+ "{fecha_devolucion}). Podés devolverla en {direccion}. {maps}"),

	RENTA_VENCIDA(
			"Hola {cliente}, tu renta venció el {fecha_devolucion}. Por favor devuélvela cuanto "
					+ "antes para evitar recargos."),

	COMPRA_REALIZADA(
			"¡Gracias por tu compra, {cliente}! Te esperamos de nuevo en {direccion}. {maps}");

	private final String textoPorDefecto;

	TipoDeEvento(String textoPorDefecto) {
		this.textoPorDefecto = textoPorDefecto;
	}

	public String textoPorDefecto() {
		return textoPorDefecto;
	}

	/** Por defecto todas las automatizaciones nacen activas; la empresa las apaga si no las quiere. */
	public boolean activaPorDefecto() {
		return true;
	}
}
