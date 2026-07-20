package com.costumi.backend.identidad.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Escalación de solicitudes de empresa vencidas (RF-15.4): reutiliza la vista de pendientes (que ya
 * marca las vencidas según el plazo configurable) y emite una alerta operable (log WARN) con las que
 * llevan demasiado sin resolver, para que el operador de la plataforma / la observabilidad las levante.
 */
@Service
class EscalarSolicitudesVencidasService implements EscalarSolicitudesVencidas {

	private static final Logger log = LoggerFactory.getLogger(EscalarSolicitudesVencidasService.class);

	private final ConsultarEmpresasPendientes pendientes;

	EscalarSolicitudesVencidasService(ConsultarEmpresasPendientes pendientes) {
		this.pendientes = pendientes;
	}

	@Override
	@Transactional(readOnly = true)
	public int ejecutar() {
		List<EmpresaPendienteVista> vencidas = pendientes.ejecutar().stream()
				.filter(EmpresaPendienteVista::vencida)
				.toList();
		if (!vencidas.isEmpty()) {
			String detalle = vencidas.stream()
					.map(v -> v.nombre() + " (" + v.id() + ")")
					.collect(Collectors.joining(", "));
			log.warn("RF-15.4: {} solicitud(es) de empresa vencida(s) sin resolver: {}", vencidas.size(), detalle);
		}
		return vencidas.size();
	}
}
