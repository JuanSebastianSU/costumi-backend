package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: listado de disfraces de la empresa (tenant). */
public interface ConsultarDisfraces {

	/** Todos los disfraces de la empresa (incluidos los archivados): vista de gestión. */
	List<Disfraz> deEmpresa(UUID empresaId);

	/** Solo los disfraces activos: vista pública (vitrina del marketplace). */
	List<Disfraz> activosDeEmpresa(UUID empresaId);
}
