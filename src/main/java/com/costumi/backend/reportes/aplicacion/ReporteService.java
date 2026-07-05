package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.IngresosReadRepository;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de Reportes (solo lectura), acotados a la empresa (tenant). */
@Service
class ReporteService implements ConsultarIngresos {

	private final IngresosReadRepository ingresos;

	ReporteService(IngresosReadRepository ingresos) {
		this.ingresos = ingresos;
	}

	@Override
	@Transactional(readOnly = true)
	public ResumenDeIngresos deEmpresa(UUID empresaId) {
		return ingresos.deEmpresa(empresaId);
	}
}
