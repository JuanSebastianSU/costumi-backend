package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.GananciaReadRepository;
import com.costumi.backend.reportes.dominio.IngresosPorMetodo;
import com.costumi.backend.reportes.dominio.IngresosReadRepository;
import com.costumi.backend.reportes.dominio.OperacionesReadRepository;
import com.costumi.backend.reportes.dominio.RentaVencida;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Casos de uso de Reportes (solo lectura), acotados a la empresa (tenant). */
@Service
class ReporteService implements ConsultarIngresos, ConsultarGanancia, ConsultarOperaciones {

	private final IngresosReadRepository ingresos;
	private final GananciaReadRepository ganancia;
	private final OperacionesReadRepository operaciones;

	ReporteService(IngresosReadRepository ingresos, GananciaReadRepository ganancia,
			OperacionesReadRepository operaciones) {
		this.ingresos = ingresos;
		this.ganancia = ganancia;
		this.operaciones = operaciones;
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

	@Override
	@Transactional(readOnly = true)
	public List<RentaVencida> rentasVencidas(UUID empresaId, UUID sucursalId) {
		return operaciones.rentasVencidas(empresaId, sucursalId, LocalDate.now());
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal depositosActivos(UUID empresaId, UUID sucursalId) {
		return operaciones.depositosActivos(empresaId, sucursalId);
	}

	@Override
	@Transactional(readOnly = true)
	public IngresosPorMetodo ingresosPorMetodo(UUID empresaId, LocalDate desde, LocalDate hasta, UUID sucursalId) {
		return operaciones.ingresosPorMetodo(empresaId, desde, hasta, sucursalId);
	}
}
