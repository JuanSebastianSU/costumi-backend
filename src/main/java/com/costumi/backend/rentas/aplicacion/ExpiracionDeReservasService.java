package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.rentas.dominio.ReservasVencidasReadRepository;
import com.costumi.backend.rentas.dominio.ReservasVencidasReadRepository.ReservaVencida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Expira las reservas no pagadas (RESERVADA sin cubrir su importe) pasadas {@code expiracion-reserva-horas}
 * (24 por defecto) de creadas, cancelándolas una por una. Cada cancelación es su propia transacción
 * (llamada al puerto {@link GestionarRenta} = proxy de Spring), así un fallo en una no frena a las demás.
 */
@Service
class ExpiracionDeReservasService implements ExpirarReservas {

	private static final Logger log = LoggerFactory.getLogger(ExpiracionDeReservasService.class);

	private final ReservasVencidasReadRepository reservas;
	private final GestionarRenta gestionarRenta;
	private final long horas;

	ExpiracionDeReservasService(ReservasVencidasReadRepository reservas, GestionarRenta gestionarRenta,
			@Value("${costumi.rentas.expiracion-reserva-horas:24}") long horas) {
		this.reservas = reservas;
		this.gestionarRenta = gestionarRenta;
		this.horas = horas;
	}

	@Override
	public int ejecutar() {
		Instant limite = Instant.now().minus(Duration.ofHours(horas));
		int canceladas = 0;
		for (ReservaVencida reserva : reservas.reservasVencidas(limite)) {
			try {
				gestionarRenta.cancelar(reserva.empresaId(), reserva.rentaId());
				canceladas++;
			}
			catch (RuntimeException e) {
				log.warn("No se pudo expirar la reserva {}: {}", reserva.rentaId(), e.getMessage());
			}
		}
		if (canceladas > 0) {
			log.info("Reservas expiradas (>{}h sin pagar): {}", horas, canceladas);
		}
		return canceladas;
	}
}
