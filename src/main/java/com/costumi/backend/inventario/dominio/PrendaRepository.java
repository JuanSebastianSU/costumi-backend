package com.costumi.backend.inventario.dominio;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Prendas (scoped por tenant). */
public interface PrendaRepository {

	Prenda guardar(Prenda prenda);

	Optional<Prenda> buscarPorId(UUID id);

	List<Prenda> listarPorEmpresa(UUID empresaId);

	/** Página de prendas de la empresa, en orden estable por nombre (C3). */
	/** Página de prendas; {@code buscar} (opcional) filtra por nombre. */
	Pagina<Prenda> listar(UUID empresaId, String buscar, SolicitudDePagina solicitud);
}
