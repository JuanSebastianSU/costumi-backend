package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.EstadoMembresia;
import com.costumi.backend.identidad.dominio.Rol;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada: la membresía de trabajo del usuario y el cambio de contexto «Comprando ↔ Trabajando»
 * (Fase B, H1). Todo por el usuario del token.
 */
public interface GestionarMembresias {

	/** Una tienda del usuario con su rol allí (para mostrar «Trabajando en …» y el historial de membresías). */
	record MembresiaConTienda(UUID empresaId, String empresaNombre, Rol rol, EstadoMembresia estado) {
	}

	/** Las membresías del usuario (su historial de trabajo; a lo sumo una ACTIVA). */
	List<MembresiaConTienda> deUsuario(UUID usuarioId);

	/** La membresía de trabajo ACTIVA del usuario, si tiene una (para «entrar a trabajar»). */
	Optional<MembresiaConTienda> activaDeUsuario(UUID usuarioId);

	/**
	 * Cambia el contexto de la sesión y emite un token nuevo (H1): {@code COMPRA} → token de cliente (sin
	 * empresa); {@code TRABAJO} → token con la empresa+rol de la membresía activa. Falla (400) si se pide
	 * TRABAJO y el usuario no tiene una membresía de trabajo activa.
	 */
	Credenciales cambiarContexto(UUID usuarioId, ModoDeSesion modo);
}
