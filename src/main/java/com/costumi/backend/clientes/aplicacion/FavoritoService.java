package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Favorito;
import com.costumi.backend.clientes.dominio.FavoritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Casos de uso de "Mis guardados" (C4): listar, guardar/actualizar y quitar favoritos del usuario. */
@Service
class FavoritoService implements GestionarFavoritos {

	private final FavoritoRepository favoritos;

	FavoritoService(FavoritoRepository favoritos) {
		this.favoritos = favoritos;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Favorito> deUsuario(UUID usuarioId) {
		return favoritos.listarPorUsuario(usuarioId);
	}

	@Override
	@Transactional
	public Favorito guardar(UUID usuarioId, UUID disfrazId, UUID empresaId, String nombre, String fotoUrl,
			BigDecimal precioRenta, BigDecimal precioVenta) {
		Favorito nuevo = Favorito.guardar(usuarioId, disfrazId, empresaId, nombre, fotoUrl, precioRenta, precioVenta);
		// Si ya lo tenía guardado, actualiza el snapshot sobre la misma fila (no duplica ni cambia su posición).
		Favorito aGuardar = favoritos.buscar(usuarioId, disfrazId).map(nuevo::conIdentidadDe).orElse(nuevo);
		return favoritos.guardar(aGuardar);
	}

	@Override
	@Transactional
	public void eliminar(UUID usuarioId, UUID disfrazId) {
		favoritos.eliminar(usuarioId, disfrazId);
	}
}
