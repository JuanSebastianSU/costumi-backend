package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.catalogo.dominio.CategoriaRepository;
import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.TipoEtiquetaRepository;
import com.costumi.backend.catalogo.dominio.ValorEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiquetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Casos de uso del motor de etiquetas, siempre acotados a la empresa (tenant). */
@Service
class TaxonomiaService implements CrearTipoEtiqueta, ConsultarTiposEtiqueta, AgregarValor, ConsultarValores,
		RenombrarTipoEtiqueta, RenombrarValor, ConsultaDeTaxonomia {

	private final TipoEtiquetaRepository tipos;
	private final ValorEtiquetaRepository valores;
	private final CategoriaRepository categorias;

	TaxonomiaService(TipoEtiquetaRepository tipos, ValorEtiquetaRepository valores, CategoriaRepository categorias) {
		this.tipos = tipos;
		this.valores = valores;
		this.categorias = categorias;
	}

	@Override
	@Transactional
	public TipoEtiqueta ejecutar(CrearTipoEtiquetaComando comando) {
		Set<UUID> categoriasQueAplica = validarCategorias(comando.empresaId(), comando.categoriasQueAplica());
		return tipos.guardar(TipoEtiqueta.crear(comando.empresaId(), comando.nombre(), comando.defineVariante(),
				comando.seleccionablePorCliente(), categoriasQueAplica));
	}

	/** Cada categoría indicada debe existir y pertenecer al tenant; si no, 400. */
	private Set<UUID> validarCategorias(UUID empresaId, List<UUID> categoriaIds) {
		Set<UUID> validas = new LinkedHashSet<>();
		for (UUID categoriaId : categoriaIds) {
			boolean delTenant = categorias.buscarPorId(categoriaId)
					.filter(categoria -> categoria.empresaId().equals(empresaId))
					.isPresent();
			if (!delTenant) {
				throw new CategoriaDeTipoInvalida("La categoría " + categoriaId + " no existe en esta empresa");
			}
			validas.add(categoriaId);
		}
		return validas;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TipoEtiqueta> deEmpresa(UUID empresaId) {
		return tipos.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional
	public ValorEtiqueta ejecutar(AgregarValorComando comando) {
		TipoEtiqueta tipo = tipoDelTenant(comando.empresaId(), comando.tipoEtiquetaId());
		return valores.guardar(ValorEtiqueta.crear(comando.empresaId(), tipo.id(), comando.valor()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ValorEtiqueta> deTipo(UUID empresaId, UUID tipoEtiquetaId) {
		TipoEtiqueta tipo = tipoDelTenant(empresaId, tipoEtiquetaId);
		return valores.listarPorTipo(tipo.id());
	}

	@Override
	@Transactional
	public TipoEtiqueta ejecutar(UUID empresaId, UUID tipoEtiquetaId, String nuevoNombre) {
		TipoEtiqueta tipo = tipoDelTenant(empresaId, tipoEtiquetaId);
		tipo.renombrar(nuevoNombre);
		return tipos.guardar(tipo);
	}

	@Override
	@Transactional
	public ValorEtiqueta ejecutar(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId, String nuevoValor) {
		tipoDelTenant(empresaId, tipoEtiquetaId);
		ValorEtiqueta valor = valores.buscarPorId(valorEtiquetaId)
				.filter(v -> v.empresaId().equals(empresaId) && v.tipoEtiquetaId().equals(tipoEtiquetaId))
				.orElseThrow(() -> new ValorEtiquetaNoEncontrado(valorEtiquetaId));
		valor.renombrar(nuevoValor);
		return valores.guardar(valor);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean categoriaExiste(UUID empresaId, UUID categoriaId) {
		return categorias.buscarPorId(categoriaId)
				.filter(categoria -> categoria.empresaId().equals(empresaId))
				.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean tipoDefineVariante(UUID empresaId, UUID tipoEtiquetaId) {
		return tipos.buscarPorId(tipoEtiquetaId)
				.filter(tipo -> tipo.empresaId().equals(empresaId))
				.map(tipo -> tipo.defineVariante() && !tipo.archivada())
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean tipoAplicaACategoria(UUID empresaId, UUID tipoEtiquetaId, UUID categoriaId) {
		return tipos.buscarPorId(tipoEtiquetaId)
				.filter(tipo -> tipo.empresaId().equals(empresaId))
				.map(tipo -> tipo.aplicaACategoria(categoriaId))
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean valorPerteneceATipo(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId) {
		boolean tipoDelTenant = tipos.buscarPorId(tipoEtiquetaId)
				.filter(tipo -> tipo.empresaId().equals(empresaId))
				.isPresent();
		if (!tipoDelTenant) {
			return false;
		}
		return valores.listarPorTipo(tipoEtiquetaId).stream()
				.anyMatch(valor -> valor.id().equals(valorEtiquetaId) && !valor.archivada());
	}

	/** Carga el tipo garantizando que pertenece a la empresa; si no, 404 (no revela existencia). */
	private TipoEtiqueta tipoDelTenant(UUID empresaId, UUID tipoEtiquetaId) {
		return tipos.buscarPorId(tipoEtiquetaId)
				.filter(tipo -> tipo.empresaId().equals(empresaId))
				.orElseThrow(() -> new TipoEtiquetaNoEncontrado(tipoEtiquetaId));
	}
}
