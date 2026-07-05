package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.Categoria;
import com.costumi.backend.catalogo.dominio.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Categorías: crear y listar, siempre acotados a la empresa (tenant). */
@Service
class CategoriaService implements CrearCategoria, ConsultarCategorias {

	private final CategoriaRepository categorias;

	CategoriaService(CategoriaRepository categorias) {
		this.categorias = categorias;
	}

	@Override
	@Transactional
	public Categoria ejecutar(CrearCategoriaComando comando) {
		return categorias.guardar(Categoria.crear(comando.empresaId(), comando.nombre()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Categoria> deEmpresa(UUID empresaId) {
		return categorias.listarPorEmpresa(empresaId);
	}
}
