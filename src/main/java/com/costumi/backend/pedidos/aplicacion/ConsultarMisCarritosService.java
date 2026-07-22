package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.CarritoAbierto;
import com.costumi.backend.pedidos.dominio.CarritosAbiertosReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Lista los carritos abiertos del usuario en todas las tiendas. Es solo lectura, sin reglas. */
@Service
class ConsultarMisCarritosService implements ConsultarMisCarritos {

	private final CarritosAbiertosReadRepository carritos;

	ConsultarMisCarritosService(CarritosAbiertosReadRepository carritos) {
		this.carritos = carritos;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CarritoAbierto> deUsuario(UUID usuarioId) {
		return carritos.deUsuario(usuarioId);
	}
}
