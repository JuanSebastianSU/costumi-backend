package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.inventario.dominio.Prenda;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Prendas de una empresa (scoped por tenant). */
public interface ConsultarPrendas {

	List<Prenda> deEmpresa(UUID empresaId);

	/** Página de prendas de la empresa, en orden estable por nombre (C3). */
	Pagina<Prenda> listar(UUID empresaId, SolicitudDePagina solicitud);
}
