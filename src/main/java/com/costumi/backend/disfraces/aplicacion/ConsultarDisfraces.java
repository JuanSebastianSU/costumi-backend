package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: listado de disfraces de la empresa (tenant). */
public interface ConsultarDisfraces {

	List<Disfraz> deEmpresa(UUID empresaId);
}
