package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.GananciaReadRepository;
import com.costumi.backend.reportes.dominio.IngresosReadRepository;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de Reportes (solo lectura), acotados a la empresa (tenant). */
@Service
class ReporteService implements ConsultarIngresos, ConsultarGanancia {

	private final IngresosReadRepository ingresos;
	private final GananciaReadRepository ganancia;

	ReporteService(IngresosReadRepository ingresos, GananciaReadRepository ganancia) {
		this.ingresos = ingresos;
		this.ganancia = ganancia;
	}

	@Override
	@Transactional(readOnly = true)
	public ResumenDeIngresos deEmpresa(UUID empresaId) {
		return ingresos.deEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public ResumenDeGanancia gananciaDeEmpresa(UUID empresaId) {
		return ganancia.deEmpresa(empresaId);
	}
}
