package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.Categoria;
import com.costumi.backend.catalogo.dominio.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Categorías: crear, listar, renombrar y archivar/activar, acotados a la empresa (tenant). */
@Service
class CategoriaService implements CrearCategoria, ConsultarCategorias, GestionarCategoria {

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

	@Override
	@Transactional
	public Categoria renombrar(UUID empresaId, UUID categoriaId, String nuevoNombre) {
		Categoria categoria = exigirDelTenant(empresaId, categoriaId);
		categoria.renombrar(nuevoNombre);
		return categorias.guardar(categoria);
	}

	@Override
	@Transactional
	public Categoria archivar(UUID empresaId, UUID categoriaId) {
		Categoria categoria = exigirDelTenant(empresaId, categoriaId);
		categoria.archivar();
		return categorias.guardar(categoria);
	}

	@Override
	@Transactional
	public Categoria activar(UUID empresaId, UUID categoriaId) {
		Categoria categoria = exigirDelTenant(empresaId, categoriaId);
		categoria.activar();
		return categorias.guardar(categoria);
	}

	private Categoria exigirDelTenant(UUID empresaId, UUID categoriaId) {
		return categorias.buscarPorId(categoriaId)
				.filter(c -> c.empresaId().equals(empresaId))
				.orElseThrow(() -> new CategoriaNoEncontrada(categoriaId));
	}
}
