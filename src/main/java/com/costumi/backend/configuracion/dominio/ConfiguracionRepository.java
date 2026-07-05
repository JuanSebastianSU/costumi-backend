package com.costumi.backend.configuracion.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de la configuración por empresa. */
public interface ConfiguracionRepository {

	ConfiguracionDeEmpresa guardar(ConfiguracionDeEmpresa configuracion);

	Optional<ConfiguracionDeEmpresa> buscarPorEmpresa(UUID empresaId);
}
