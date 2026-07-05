package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.inventario.dominio.EtiquetasDePrenda;
import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso de Prendas: crear y listar, acotados a la empresa (tenant). */
@Service
class PrendaService implements CrearPrenda, ConsultarPrendas {

	private final PrendaRepository prendas;
	private final ConsultaDeTaxonomia taxonomia;

	PrendaService(PrendaRepository prendas, ConsultaDeTaxonomia taxonomia) {
		this.prendas = prendas;
		this.taxonomia = taxonomia;
	}

	@Override
	@Transactional
	public Prenda ejecutar(CrearPrendaComando comando) {
		EtiquetasDePrenda etiquetas = validarEtiquetas(comando.empresaId(), comando.etiquetas());
		return prendas.guardar(Prenda.crear(comando.empresaId(), comando.categoriaId(), comando.nombre(),
				comando.tipoArticulo(), comando.precioRenta(), comando.precioVenta(), etiquetas));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Prenda> deEmpresa(UUID empresaId) {
		return prendas.listarPorEmpresa(empresaId);
	}

	/**
	 * Valida que cada etiqueta referencie un valor real de la taxonomía del tenant y que no se repita
	 * una dimensión. Cualquier tipo/valor sirve para clasificar (no se exige que "defina variante").
	 */
	private EtiquetasDePrenda validarEtiquetas(UUID empresaId, List<EtiquetaSeleccionada> etiquetas) {
		Map<UUID, UUID> mapa = new LinkedHashMap<>();
		for (EtiquetaSeleccionada etiqueta : etiquetas) {
			if (etiqueta.tipoEtiquetaId() == null || etiqueta.valorEtiquetaId() == null) {
				throw new EtiquetaDePrendaInvalida("Cada etiqueta requiere tipo y valor");
			}
			if (mapa.containsKey(etiqueta.tipoEtiquetaId())) {
				throw new EtiquetaDePrendaInvalida("La prenda no puede repetir la misma dimensión de etiqueta");
			}
			if (!taxonomia.valorPerteneceATipo(empresaId, etiqueta.tipoEtiquetaId(), etiqueta.valorEtiquetaId())) {
				throw new EtiquetaDePrendaInvalida("El valor no pertenece al tipo de etiqueta indicado");
			}
			mapa.put(etiqueta.tipoEtiquetaId(), etiqueta.valorEtiquetaId());
		}
		return EtiquetasDePrenda.de(mapa);
	}
}
