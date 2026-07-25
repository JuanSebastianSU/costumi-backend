package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
				.map(this::conTienda)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MembresiaConTienda> activaDeUsuario(UUID usuarioId) {
		return membresias.activaDeUsuario(usuarioId).map(this::conTienda);
	}

	@Override
	@Transactional
	public Credenciales cambiarContexto(UUID usuarioId, ModoDeSesion modo) {
		Usuario usuario = usuarios.buscarPorId(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("La cuenta no existe"));
		if (!usuario.activo()) {
			throw new CuentaDesactivada();
		}
		Usuario enContexto = (modo == ModoDeSesion.TRABAJO) ? proyectarATrabajo(usuario) : proyectarACompra(usuario);
		return sesiones.nuevaSesion(enContexto);
	}

	/** «Trabajando»: proyecta la identidad a la empresa+rol de la membresía activa (400 si no tiene una). */
	private Usuario proyectarATrabajo(Usuario usuario) {
		Membresia membresia = membresias.activaDeUsuario(usuario.id())
				.orElseThrow(() -> new IllegalArgumentException("No tenés una membresía de trabajo activa"));
		return Usuario.rehidratar(usuario.id(), membresia.empresaId(), usuario.email(), usuario.passwordHash(),
				membresia.rol(), usuario.activo(), usuario.nombre(), usuario.telefono(), usuario.fotoUrl());
	}

	/** «Comprando»: proyecta la identidad a cliente (sin empresa) — la faceta que toda persona tiene (H1). */
	private Usuario proyectarACompra(Usuario usuario) {
		return Usuario.rehidratar(usuario.id(), null, usuario.email(), usuario.passwordHash(),
				Rol.CLIENTE, usuario.activo(), usuario.nombre(), usuario.telefono(), usuario.fotoUrl());
	}

	private MembresiaConTienda conTienda(Membresia m) {
		return new MembresiaConTienda(m.empresaId(), nombreDe(m.empresaId()), m.rol(), m.estado());
	}

	private String nombreDe(UUID empresaId) {
		return empresas.buscarPorId(empresaId).map(Empresa::nombre).orElse(null);
	}
}
