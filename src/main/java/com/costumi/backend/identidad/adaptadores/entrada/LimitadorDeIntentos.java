package com.costumi.backend.identidad.adaptadores.entrada;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador de intentos en memoria (A2): ventana fija por clave. Protege los endpoints de {@code /auth}
 * (login, olvidé, registro) contra fuerza bruta y abuso, acotando cuántos intentos se aceptan por
 * ventana. La clave incluye el <b>email</b>, así el límite es por cuenta.
 *
 * <p><b>Alcance / limitaciones (documentadas):</b> el conteo vive en memoria del proceso; si se escala a
 * varias instancias, el límite es por instancia (aún así encarece el ataque). No cubre fuerza bruta
 * <i>distribuida</i> por muchas cuentas desde una IP (eso requeriría además límite por IP con la IP real
 * del proxy). Para límite global compartido, migrar a un store (Redis) e implementar la misma interfaz.
 */
@Component
class LimitadorDeIntentos {

	private final int maximo;
	private final long ventanaMillis;
	private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

	LimitadorDeIntentos(@Value("${costumi.security.rate-limit.auth.max:10}") int maximo,
			@Value("${costumi.security.rate-limit.auth.window-seconds:60}") long ventanaSegundos) {
		this.maximo = maximo;
		this.ventanaMillis = ventanaSegundos * 1000L;
	}

	/** Registra un intento para {@code clave} y devuelve {@code true} si está dentro del límite. */
	boolean permitir(String clave) {
		if (maximo <= 0) {
			return true; // deshabilitado por configuración
		}
		long ahora = System.currentTimeMillis();
		Ventana ventana = ventanas.compute(clave, (k, actual) -> {
			if (actual == null || ahora - actual.inicio >= ventanaMillis) {
				return new Ventana(ahora, new AtomicInteger(1));
			}
			actual.conteo.incrementAndGet();
			return actual;
		});
		return ventana.conteo.get() <= maximo;
	}

	private static final class Ventana {
		private final long inicio;
		private final AtomicInteger conteo;

		private Ventana(long inicio, AtomicInteger conteo) {
			this.inicio = inicio;
			this.conteo = conteo;
		}
	}
}
