package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Membresías del usuario y cambio de contexto (Fase B). Aditivo: no toca el login ni el Usuario base. */
@Service
class MembresiaService implements GestionarMembresias {

	private final MembresiaRepository membresias;
	private final UsuarioRepository usuarios;
	private final EmpresaRepository empresas;
	private final EmisorDeSesion sesiones;

	MembresiaService(MembresiaRepository membresias, UsuarioRepository usuarios, EmpresaRepository empresas,
			EmisorDeSesion sesiones) {
		this.membresias = membresias;
		this.usuarios = usuarios;
		this.empresas = empresas;
		this.sesiones = sesiones;
	}

	@Override
	@Transactional(readOnly = true)
	public List<MembresiaConTienda> deUsuario(UUID usuarioId) {
		return membresias.deUsuario(usuarioId).stream()
				.map(m -> new MembresiaConTienda(m.empresaId(), nombreDe(m.empresaId()), m.rol(), m.estado()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Credenciales cambiarContexto(UUID usuarioId, UUID empresaId) {
		Membresia membresia = membresias.buscar(usuarioId, empresaId)
				.filter(Membresia::activa)
				.orElseThrow(() -> new IllegalArgumentException("No tenés una membresía activa en esa tienda"));
		Usuario usuario = usuarios.buscarPorId(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("La cuenta no existe"));
		if (!usuario.activo()) {
			throw new CuentaDesactivada();
		}
		// Usuario "proyectado" al contexto elegido (misma identidad; empresa+rol de la membresía) → token nuevo.
		Usuario enContexto = Usuario.rehidratar(usuario.id(), empresaId, usuario.email(), usuario.passwordHash(),
				membresia.rol(), usuario.activo(), usuario.nombre(), usuario.telefono(), usuario.fotoUrl());
		return sesiones.nuevaSesion(enContexto);
	}

	private String nombreDe(UUID empresaId) {
		return empresas.buscarPorId(empresaId).map(Empresa::nombre).orElse(null);
	}
}
