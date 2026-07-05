package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Prendas: crear y listar, acotados a la empresa (tenant). */
@Service
class PrendaService implements CrearPrenda, ConsultarPrendas {

	private final PrendaRepository prendas;

	PrendaService(PrendaRepository prendas) {
		this.prendas = prendas;
	}

	@Override
	@Transactional
	public Prenda ejecutar(CrearPrendaComando comando) {
		return prendas.guardar(Prenda.crear(comando.empresaId(), comando.categoriaId(), comando.nombre(),
				comando.tipoArticulo(), comando.precioRenta(), comando.precioVenta()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Prenda> deEmpresa(UUID empresaId) {
		return prendas.listarPorEmpresa(empresaId);
	}
}
