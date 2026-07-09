package com.costumi.backend.configuracion.adaptadores.salida;

import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;
import com.costumi.backend.configuracion.dominio.ConfiguracionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link ConfiguracionRepository} con JPA. */
@Repository
class ConfiguracionRepositoryAdapter implements ConfiguracionRepository {

	private final ConfiguracionJpaRepository jpa;

	ConfiguracionRepositoryAdapter(ConfiguracionJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public ConfiguracionDeEmpresa guardar(ConfiguracionDeEmpresa c) {
		ConfiguracionJpaEntity guardada = jpa.save(new ConfiguracionJpaEntity(c.empresaId(), c.conteoStock(),
				c.multasActivo(), c.multiSucursal(), c.pagoEnLinea(), c.tasaImpuesto(), c.moneda(),
				c.recargoPorRetrasoPorDia(), c.modoRecargoRetraso(), c.reembolsosActivos(), c.ventanaReembolsoDias()));
		return aDominio(guardada);
	}

	@Override
	public Optional<ConfiguracionDeEmpresa> buscarPorEmpresa(UUID empresaId) {
		return jpa.findById(empresaId).map(ConfiguracionRepositoryAdapter::aDominio);
	}

	private static ConfiguracionDeEmpresa aDominio(ConfiguracionJpaEntity e) {
		return ConfiguracionDeEmpresa.de(e.getEmpresaId(), e.isConteoStock(), e.isMultasActivo(),
				e.isMultiSucursal(), e.isPagoEnLinea(), e.getTasaImpuesto(), e.getMoneda(),
				e.getRecargoPorRetrasoPorDia(), e.getModoRecargoRetraso(), e.isReembolsosActivos(),
				e.getVentanaReembolsoDias());
	}
}
