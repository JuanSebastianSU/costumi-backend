package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.TipoEtiquetaRepository;
import com.costumi.backend.catalogo.dominio.ValorEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiquetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso del motor de etiquetas, siempre acotados a la empresa (tenant). */
@Service
class TaxonomiaService implements CrearTipoEtiqueta, ConsultarTiposEtiqueta, AgregarValor, ConsultarValores {

	private final TipoEtiquetaRepository tipos;
	private final ValorEtiquetaRepository valores;

	TaxonomiaService(TipoEtiquetaRepository tipos, ValorEtiquetaRepository valores) {
		this.tipos = tipos;
		this.valores = valores;
	}

	@Override
	@Transactional
	public TipoEtiqueta ejecutar(CrearTipoEtiquetaComando comando) {
		return tipos.guardar(TipoEtiqueta.crear(
				comando.empresaId(), comando.nombre(), comando.defineVariante(), comando.seleccionablePorCliente()));
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

	/** Carga el tipo garantizando que pertenece a la empresa; si no, 404 (no revela existencia). */
	private TipoEtiqueta tipoDelTenant(UUID empresaId, UUID tipoEtiquetaId) {
		return tipos.buscarPorId(tipoEtiquetaId)
				.filter(tipo -> tipo.empresaId().equals(empresaId))
				.orElseThrow(() -> new TipoEtiquetaNoEncontrado(tipoEtiquetaId));
	}
}
