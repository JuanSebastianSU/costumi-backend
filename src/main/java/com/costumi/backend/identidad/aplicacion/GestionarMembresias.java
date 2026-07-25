package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.EstadoMembresia;
import com.costumi.backend.identidad.dominio.Rol;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: las tiendas a las que pertenece el usuario y el cambio de contexto (Fase B). Todo por
 * el usuario del token.
 */
public interface GestionarMembresias {

	/** Una tienda del usuario con su rol allí (para la pantalla "elegir tienda"). */
	record MembresiaConTienda(UUID empresaId, String empresaNombre, Rol rol, EstadoMembresia estado) {
	}

	/** Las membresías del usuario (todas sus tiendas). */
	List<MembresiaConTienda> deUsuario(UUID usuarioId);

	/**
	 * Cambia el contexto activo a la tienda dada: valida que el usuario tenga una membresía ACTIVA ahí y
	 * emite una sesión nueva (token) con esa empresa+rol. Falla si no tiene membresía activa en esa empresa.
	 */
	Credenciales cambiarContexto(UUID usuarioId, UUID empresaId);
}
