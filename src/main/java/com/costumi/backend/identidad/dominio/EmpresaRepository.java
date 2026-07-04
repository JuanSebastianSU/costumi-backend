package com.costumi.backend.identidad.dominio;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: persistencia de Empresas. Lo implementa un adaptador en
 * {@code adaptadores.salida}. El dominio define la interfaz; no conoce la tecnología.
 */
public interface EmpresaRepository {

	Empresa guardar(Empresa empresa);

	Optional<Empresa> buscarPorId(UUID id);
}
