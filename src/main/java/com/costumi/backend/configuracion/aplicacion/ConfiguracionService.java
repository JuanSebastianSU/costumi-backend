package com.costumi.backend.configuracion.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;
import com.costumi.backend.configuracion.dominio.ConfiguracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de configuración, acotados a la empresa (tenant). */
@Service
class ConfiguracionService implements GestionarConfiguracion, ConsultaDeConfiguracion {

	private final ConfiguracionRepository configuraciones;

	ConfiguracionService(ConfiguracionRepository configuraciones) {
		this.configuraciones = configuraciones;
	}

	@Override
	@Transactional(readOnly = true)
	public ConfiguracionDeEmpresa deEmpresa(UUID empresaId) {
		return configuraciones.buscarPorEmpresa(empresaId)
				.orElseGet(() -> ConfiguracionDeEmpresa.porDefecto(empresaId));
	}

	@Override
	@Transactional
	public ConfiguracionDeEmpresa actualizar(ActualizarConfiguracionComando comando) {
		return configuraciones.guardar(ConfiguracionDeEmpresa.de(comando.empresaId(), comando.conteoStock(),
				comando.multasActivo(), comando.multiSucursal(), comando.pagoEnLinea(), comando.tasaImpuesto(),
				comando.moneda(), comando.recargoPorRetrasoPorDia(), comando.modoRecargoRetraso()));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean multasActivas(UUID empresaId) {
		return deEmpresa(empresaId).multasActivo();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean conteoStock(UUID empresaId) {
		return deEmpresa(empresaId).conteoStock();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean multiSucursal(UUID empresaId) {
		return deEmpresa(empresaId).multiSucursal();
	}

	@Override
	@Transactional(readOnly = true)
	public java.math.BigDecimal tasaImpuesto(UUID empresaId) {
		return deEmpresa(empresaId).tasaImpuesto();
	}

	@Override
	@Transactional(readOnly = true)
	public java.math.BigDecimal recargoPorRetrasoPorDia(UUID empresaId) {
		return deEmpresa(empresaId).recargoPorRetrasoPorDia();
	}

	@Override
	@Transactional(readOnly = true)
	public java.math.BigDecimal recargoPorRetraso(UUID empresaId, long diasAtraso) {
		return deEmpresa(empresaId).recargoPorRetraso(diasAtraso);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean pagoEnLinea(UUID empresaId) {
		return deEmpresa(empresaId).pagoEnLinea();
	}
}
