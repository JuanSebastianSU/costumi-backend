package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.EstadoEmpresa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Lista las empresas PENDIENTES y marca las vencidas según el plazo configurable (RF-15.4). */
@Service
class ConsultarEmpresasPendientesService implements ConsultarEmpresasPendientes {

	private final EmpresaRepository empresas;
	private final Duration plazoResolucion;

	ConsultarEmpresasPendientesService(EmpresaRepository empresas,
			@Value("${costumi.empresa.plazo-resolucion-dias:2}") long plazoResolucionDias) {
		this.empresas = empresas;
		this.plazoResolucion = Duration.ofDays(plazoResolucionDias);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmpresaPendienteVista> ejecutar() {
		Instant ahora = Instant.now();
		return empresas.listarPorEstado(EstadoEmpresa.PENDIENTE).stream()
				.map(empresa -> new EmpresaPendienteVista(
						empresa.id(), empresa.nombre(), empresa.fechaRegistro(),
						empresa.solicitudVencida(plazoResolucion, ahora)))
				.toList();
	}
}
