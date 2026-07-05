package com.costumi.backend.configuracion.aplicacion;

import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;

import java.util.UUID;

/** Puerto de entrada: consultar y actualizar la configuración de la empresa (RF-12.4). */
public interface GestionarConfiguracion {

	ConfiguracionDeEmpresa deEmpresa(UUID empresaId);

	ConfiguracionDeEmpresa actualizar(ActualizarConfiguracionComando comando);
}
