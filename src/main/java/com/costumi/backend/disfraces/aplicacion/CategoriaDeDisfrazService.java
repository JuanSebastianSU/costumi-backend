package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz;
import com.costumi.backend.disfraces.dominio.CategoriaDeDisfrazRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de las Categorías de disfraz, acotados a la empresa (tenant). */
@Service
class CategoriaDeDisfrazService implements GestionCategoriasDeDisfraz {

	private final CategoriaDeDisfrazRepository categorias;

	CategoriaDeDisfrazService(CategoriaDeDisfrazRepository categorias) {
		this.categorias = categorias;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoriaDeDisfraz> deEmpresa(UUID empresaId) {
		return categorias.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional
	public CategoriaDeDisfraz crear(UUID empresaId, String nombre) {
		return categorias.guardar(CategoriaDeDisfraz.crear(empresaId, nombre));
	}

	@Override
	@Transactional
	public CategoriaDeDisfraz renombrar(UUID empresaId, UUID categoriaId, String nuevoNombre) {
		CategoriaDeDisfraz categoria = exigirDelTenant(empresaId, categoriaId);
		categoria.renombrar(nuevoNombre);
		return categorias.guardar(categoria);
	}

	@Override
	@Transactional
	public CategoriaDeDisfraz archivar(UUID empresaId, UUID categoriaId) {
		CategoriaDeDisfraz categoria = exigirDelTenant(empresaId, categoriaId);
		categoria.archivar();
		return categorias.guardar(categoria);
	}

	@Override
	@Transactional
	public CategoriaDeDisfraz activar(UUID empresaId, UUID categoriaId) {
		CategoriaDeDisfraz categoria = exigirDelTenant(empresaId, categoriaId);
		categoria.activar();
		return categorias.guardar(categoria);
	}

	private CategoriaDeDisfraz exigirDelTenant(UUID empresaId, UUID categoriaId) {
		return categorias.buscarPorId(categoriaId)
				.filter(c -> c.empresaId().equals(empresaId))
				.orElseThrow(() -> new CategoriaDeDisfrazNoEncontrada(categoriaId));
	}
}
