package com.costumi.backend.identidad.adaptadores.entrada;

/**
 * DTO de salida de la identidad del usuario autenticado (H1). Expresa la <b>persona</b> (id, email) y el
 * <b>contexto actual</b> del token (rol/empresa; para cliente van null), más su <b>membresía de trabajo
 * activa</b> si tiene una — así la app sabe si ofrecer el switch «Comprando ↔ Trabajando en …».
 */
public record UsuarioActualResponse(String id, String email, String rol, String empresaId,
		MembresiaActiva membresiaActiva) {

	/** La membresía de trabajo activa de la persona (independiente del contexto actual del token). */
	public record MembresiaActiva(String empresaId, String empresaNombre, String rol) {
	}
}
