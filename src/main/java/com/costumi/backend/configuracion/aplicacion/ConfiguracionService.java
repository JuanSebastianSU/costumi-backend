package com.costumi.backend.configuracion.aplicacion;

import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;
import com.costumi.backend.configuracion.dominio.ConfiguracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de configuración, acotados a la empresa (tenant). */
@Service
class ConfiguracionService implements GestionarConfiguracion {

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
				comando.multasActivo(), comando.multiSucursal(), comando.pagoEnLinea()));
	}
}
