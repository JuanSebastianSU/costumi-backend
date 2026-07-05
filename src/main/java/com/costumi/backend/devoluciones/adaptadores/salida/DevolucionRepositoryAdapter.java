package com.costumi.backend.devoluciones.adaptadores.salida;

import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.devoluciones.dominio.DevolucionRepository;
import com.costumi.backend.devoluciones.dominio.PiezaRevisada;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: persiste la Devolución (cabecera + checklist) con JPA. */
@Repository
class DevolucionRepositoryAdapter implements DevolucionRepository {

	private final DevolucionJpaRepository cabeceras;
	private final PiezaRevisadaJpaRepository piezas;

	DevolucionRepositoryAdapter(DevolucionJpaRepository cabeceras, PiezaRevisadaJpaRepository piezas) {
		this.cabeceras = cabeceras;
		this.piezas = piezas;
	}

	@Override
	public Devolucion guardar(Devolucion devolucion) {
		cabeceras.save(new DevolucionJpaEntity(devolucion.id(), devolucion.empresaId(), devolucion.rentaId(),
				devolucion.deposito(), devolucion.cargoPorDanos(), devolucion.cargoPorRetraso(),
				devolucion.remanente()));
		for (PiezaRevisada pieza : devolucion.piezas()) {
			piezas.save(new PiezaRevisadaJpaEntity(UUID.randomUUID(), devolucion.id(), devolucion.empresaId(),
					pieza.descripcion(), pieza.llego(), pieza.estado()));
		}
		return devolucion;
	}

	@Override
	public Optional<Devolucion> buscarPorId(UUID id) {
		return cabeceras.findById(id).map(this::aDominio);
	}

	@Override
	public List<Devolucion> listarPorEmpresa(UUID empresaId) {
		return cabeceras.findByEmpresaId(empresaId).stream().map(this::aDominio).toList();
	}

	private Devolucion aDominio(DevolucionJpaEntity cabecera) {
		List<PiezaRevisada> checklist = piezas.findByDevolucionId(cabecera.getId()).stream()
				.map(p -> PiezaRevisada.de(p.getDescripcion(), p.isLlego(), p.getEstado()))
				.toList();
		return Devolucion.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getRentaId(),
				cabecera.getDeposito(), cabecera.getCargoPorDanos(), cabecera.getCargoPorRetraso(),
				cabecera.getRemanente(), checklist);
	}
}
