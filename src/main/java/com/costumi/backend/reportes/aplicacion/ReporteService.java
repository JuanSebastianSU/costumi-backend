package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ArticuloRanking;
import com.costumi.backend.reportes.dominio.EmpleadoVentas;
import com.costumi.backend.reportes.dominio.GananciaReadRepository;
import com.costumi.backend.reportes.dominio.GrupoInventario;
import com.costumi.backend.reportes.dominio.IngresosPorMetodo;
import com.costumi.backend.reportes.dominio.IngresosReadRepository;
import com.costumi.backend.reportes.dominio.InventarioReadRepository;
import com.costumi.backend.reportes.dominio.OperacionesReadRepository;
import com.costumi.backend.reportes.dominio.RankingReadRepository;
import com.costumi.backend.reportes.dominio.RentaVencida;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import com.costumi.backend.reportes.dominio.ResumenInventario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Casos de uso de Reportes (solo lectura), acotados a la empresa (tenant). */
@Service
class ReporteService
		implements ConsultarIngresos, ConsultarGanancia, ConsultarOperaciones, ConsultarRankings, ConsultarInventario {

	private final IngresosReadRepository ingresos;
	private final GananciaReadRepository ganancia;
	private final OperacionesReadRepository operaciones;
	private final RankingReadRepository rankings;
	private final InventarioReadRepository inventario;

	ReporteService(IngresosReadRepository ingresos, GananciaReadRepository ganancia,
			OperacionesReadRepository operaciones, RankingReadRepository rankings,
			InventarioReadRepository inventario) {
		this.ingresos = ingresos;
		this.ganancia = ganancia;
		this.operaciones = operaciones;
		this.rankings = rankings;
		this.inventario = inventario;
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

	@Override
	@Transactional(readOnly = true)
	public List<ArticuloRanking> masVendidos(UUID empresaId, UUID sucursalId, int limite) {
		return rankings.masVendidos(empresaId, sucursalId, limite);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ArticuloRanking> masRentados(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta,
			int limite) {
		return rankings.masRentados(empresaId, sucursalId, desde, hasta, limite);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmpleadoVentas> ventasPorEmpleado(UUID empresaId, UUID sucursalId) {
		return rankings.ventasPorEmpleado(empresaId, sucursalId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<GrupoInventario> tableroDeInventario(UUID empresaId) {
		return inventario.tablero(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public ResumenInventario resumenDeInventario(UUID empresaId) {
		return inventario.resumen(empresaId);
	}
}
