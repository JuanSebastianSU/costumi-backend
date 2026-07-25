package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

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
}
