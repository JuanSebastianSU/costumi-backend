package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Proyecta la sesión de una persona según su membresía de trabajo (H1, Fase B). Si su rol base es de trabajo
 * pero <b>tiene membresías y ninguna está activa</b> (fue suspendido o dado de baja), la sesión cae a
 * <b>cliente</b> —así la desvinculación corta el acceso sin tocar el {@code Usuario} base—. Si no tiene
 * membresías (cuentas heredadas/semilla) o alguna está activa, opera con su contexto base como siempre.
 */
@Component
class ContextoDeTrabajo {

	private final MembresiaRepository membresias;

	ContextoDeTrabajo(MembresiaRepository membresias) {
		this.membresias = membresias;
	}

	Usuario paraSesion(Usuario base) {
		if (!base.rol().requiereEmpresa()) {
			return base; // cliente o superadmin: no depende de membresía
		}
		List<Membresia> suyas = membresias.deUsuario(base.id());
		boolean tieneMembresias = !suyas.isEmpty();
		boolean algunaActiva = suyas.stream().anyMatch(Membresia::activa);
		if (tieneMembresias && !algunaActiva) {
			return base.volverASoloCliente(); // desvinculado: opera como cliente
		}
		return base;
	}

	/**
	 * Proyecta la sesión al refrescar preservando el <b>modo</b> que llevaba el token (Fase B): si venía en
	 * compra (rol cliente) sigue en compra; si venía trabajando en una empresa, re-valida que la membresía
	 * siga ACTIVA con ese rol y la mantiene (si ya no lo está, cae a cliente). Cualquier otro caso (token base)
	 * usa la proyección normal.
	 */
	Usuario paraRefresh(Usuario base, String empresaIdClaim, String rolClaim) {
		if (Rol.CLIENTE.name().equals(rolClaim)) {
			return base.rol().esCliente() ? base : base.volverASoloCliente(); // modo compra preservado
		}
		if (empresaIdClaim != null && rolClaim != null) {
			Optional<Membresia> m = membresiaContexto(base.id(), empresaIdClaim, rolClaim);
			if (m.isPresent()) {
				Membresia mem = m.get();
				return Usuario.rehidratar(base.id(), mem.empresaId(), base.email(), base.passwordHash(),
						mem.rol(), base.activo(), base.nombre(), base.telefono(), base.fotoUrl());
			}
		}
		return paraSesion(base); // token base o membresía ya no activa
	}

	private Optional<Membresia> membresiaContexto(UUID usuarioId, String empresaIdClaim, String rolClaim) {
		try {
			UUID empresaId = UUID.fromString(empresaIdClaim);
			Rol rol = Rol.valueOf(rolClaim);
			return membresias.buscar(usuarioId, empresaId).filter(Membresia::activa).filter(mem -> mem.rol() == rol);
		} catch (IllegalArgumentException e) {
			return Optional.empty(); // claims con formato inesperado: caer al contexto base
		}
	}
}
