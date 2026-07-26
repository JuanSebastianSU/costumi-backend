package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Invitacion;
import com.costumi.backend.identidad.dominio.InvitacionRepository;
import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementa las invitaciones de trabajo (Fase B, paso 3). Invitar valida rol/pirámide/sucursales y la
 * regla de una-membresía-activa; aceptar registra los T&C, crea la cuenta como CLIENTE si no existe (H1: la
 * persona es siempre cliente; el rol de trabajo vive en la membresía), crea la membresía ACTIVA y asigna las
 * sucursales. El token viaja por email/enlace; se guarda solo su hash.
 */
@Service
class InvitacionService implements GestionarInvitaciones {

	private final InvitacionRepository invitaciones;
	private final MembresiaRepository membresias;
	private final UsuarioRepository usuarios;
	private final EmpresaRepository empresas;
	private final AsignacionDeSucursalesRepository asignaciones;
	private final AutoridadSobreSucursales autoridadSobreSucursales;
	private final PasswordEncoder passwordEncoder;
	private final EnviadorDeEmail email;
	private final EmisorDeSesion sesiones;
	private final Duration duracion;
	private final String urlBase;

	InvitacionService(InvitacionRepository invitaciones, MembresiaRepository membresias, UsuarioRepository usuarios,
			EmpresaRepository empresas, AsignacionDeSucursalesRepository asignaciones,
			AutoridadSobreSucursales autoridadSobreSucursales, PasswordEncoder passwordEncoder, EnviadorDeEmail email,
			EmisorDeSesion sesiones,
			@Value("${costumi.invitacion.duracion-dias:7}") long duracionDias,
			@Value("${costumi.invitacion.url-base:}") String urlBase) {
		this.invitaciones = invitaciones;
		this.membresias = membresias;
		this.usuarios = usuarios;
		this.empresas = empresas;
		this.asignaciones = asignaciones;
		this.autoridadSobreSucursales = autoridadSobreSucursales;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.sesiones = sesiones;
		this.duracion = Duration.ofDays(duracionDias);
		this.urlBase = urlBase;
	}

	@Override
	@Transactional
	public InvitacionCreada invitar(InvitarComando comando) {
		Rol rol = comando.rol();
		if (rol == Rol.SUPERADMIN || rol == Rol.CLIENTE) {
			throw new IllegalArgumentException("El rol de una invitación debe ser de la empresa (no SuperAdmin ni Cliente)");
		}
		if (!comando.actorRol().puedeGestionarA(rol)) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		String correo = normalizar(comando.email());
		Set<UUID> sucursalIds = Set.copyOf(comando.sucursalIds());
		autoridadSobreSucursales.exigirAsignables(comando.empresaId(), comando.actorRol(), comando.actorId(), sucursalIds);
		// Regla de seguridad #2: no se puede invitar a quien ya trabaja en una tienda (debe salir primero).
		usuarios.buscarPorEmail(correo).ifPresent(u -> membresias.activaDeUsuario(u.id()).ifPresent(m -> {
			throw new IllegalArgumentException("La persona ya trabaja en una tienda; debe desvincularse antes de aceptar otra");
		}));
		// Una sola invitación pendiente por email/empresa: re-invitar reemplaza la anterior.
		invitaciones.pendientePorEmailYEmpresa(correo, comando.empresaId())
				.ifPresent(previa -> invitaciones.guardar(previa.cancelar()));

		String token = TokenAleatorio.generar();
		Invitacion invitacion = invitaciones.guardar(Invitacion.crear(comando.empresaId(), correo, rol, sucursalIds,
				TokenAleatorio.hash(token), Instant.now().plus(duracion)));
		String enlace = enlaceDe(token);
		email.enviar(correo, "Te invitaron a trabajar en Costumi", cuerpo(comando.empresaId(), rol, enlace));
		return new InvitacionCreada(invitacion.id(), correo, rol, token, enlace);
	}

	@Override
	@Transactional
	public InvitacionCreada reenviar(UUID empresaId, Rol actorRol, UUID actorId, UUID invitacionId) {
		Invitacion invitacion = invitaciones.buscarPorId(invitacionId)
				.filter(i -> i.empresaId().equals(empresaId))
				.orElseThrow(() -> new IllegalArgumentException("La invitación no existe en esta empresa"));
		// Reusa invitar: cancela la pendiente y crea una nueva (token/enlace nuevo) con el mismo email, rol
		// y sucursales, y vuelve a mandar el email. Valida rol/pirámide/sucursales igual que al invitar.
		return invitar(new InvitarComando(empresaId, actorRol, actorId, invitacion.email(), invitacion.rol(),
				invitacion.sucursalIds()));
	}

	@Override
	@Transactional(readOnly = true)
	public InvitacionVista ver(String token) {
		Invitacion invitacion = vigentePorToken(token);
		String nombre = empresas.buscarPorId(invitacion.empresaId()).map(Empresa::nombre).orElse(null);
		boolean necesitaCuenta = usuarios.buscarPorEmail(invitacion.email()).isEmpty();
		return new InvitacionVista(nombre, invitacion.rol(), invitacion.email(), necesitaCuenta);
	}

	@Override
	@Transactional
	public Credenciales aceptar(AceptarComando comando) {
		if (!comando.aceptaTerminos()) {
			throw new IllegalArgumentException("Debés aceptar los términos y condiciones para unirte");
		}
		Invitacion invitacion = vigentePorToken(comando.token());
		Usuario persona = vincularOCrear(invitacion, comando.password());
		// Membresía de trabajo ACTIVA (el adapter reusa la fila si ya existía una baja en esta empresa).
		membresias.guardar(Membresia.crear(persona.id(), invitacion.empresaId(), invitacion.rol()));
		asignaciones.reemplazar(invitacion.empresaId(), persona.id(), invitacion.sucursalIds());
		invitaciones.guardar(invitacion.aceptar(Instant.now()));
		return sesiones.nuevaSesion(persona);
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvitacionPendiente> pendientes(UUID empresaId) {
		return invitaciones.pendientesDeEmpresa(empresaId).stream()
				.map(i -> new InvitacionPendiente(i.id(), i.email(), i.rol(), i.expiraEn()))
				.toList();
	}

	@Override
	@Transactional
	public void cancelar(UUID empresaId, Rol actorRol, UUID invitacionId) {
		Invitacion invitacion = invitaciones.buscarPorId(invitacionId)
				.filter(i -> i.empresaId().equals(empresaId))
				.orElseThrow(() -> new IllegalArgumentException("La invitación no existe en esta empresa"));
		if (!actorRol.puedeGestionarA(invitacion.rol())) {
			throw new GestionDeEmpleadoNoPermitida();
		}
		invitaciones.guardar(invitacion.cancelar());
	}

	/**
	 * Resuelve la persona de la invitación: si tiene cuenta, la vincula como empleado (valida que no trabaje ya
	 * en otra tienda); si no, la crea. Queda con contexto base empresa+rol —como el alta directa anterior— para
	 * que la gestión de personal (listado, roles, sucursales) la encuentre; igual puede «Comprar» con el switch.
	 */
	private Usuario vincularOCrear(Invitacion invitacion, String password) {
		return usuarios.buscarPorEmail(invitacion.email())
				.map(existente -> {
					if (!existente.activo()) {
						throw new CuentaDesactivada();
					}
					membresias.activaDeUsuario(existente.id()).ifPresent(m -> {
						throw new IllegalArgumentException("Ya trabajás en una tienda; salí de la actual antes de aceptar otra");
					});
					return usuarios.guardar(existente.vincularComoEmpleado(invitacion.empresaId(), invitacion.rol()));
				})
				.orElseGet(() -> {
					if (password == null || password.length() < 8) {
						throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
					}
					return usuarios.guardar(Usuario.crear(invitacion.empresaId(), invitacion.email(),
							passwordEncoder.encode(password), invitacion.rol()));
				});
	}

	private Invitacion vigentePorToken(String token) {
		return invitaciones.buscarPorHash(TokenAleatorio.hash(token))
				.filter(i -> i.vigente(Instant.now()))
				.orElseThrow(() -> new IllegalArgumentException("La invitación es inválida o venció"));
	}

	private String enlaceDe(String token) {
		return urlBase.isBlank() ? token : urlBase + "?token=" + token;
	}

	private String cuerpo(UUID empresaId, Rol rol, String enlace) {
		String nombre = empresas.buscarPorId(empresaId).map(Empresa::nombre).orElse("una tienda");
		// Sin URL base, `enlace` es un código (no un link): decir "abrí este enlace" confunde porque no se
		// puede abrir. Se dan instrucciones claras para usarlo dentro de la app. Con URL base, sí es un link.
		String comoAceptar = urlBase.isBlank()
				? "Para aceptar, abrí la app Costumi, tocá \"Tengo una invitación\" e ingresá este código:\n\n"
						+ enlace
				: "Para aceptar (y aceptar los términos y condiciones), abrí este enlace:\n" + enlace;
		return "Te invitaron a trabajar en " + nombre + " como " + rol.name() + " en Costumi.\n\n"
				+ comoAceptar
				+ "\n\nLa invitación vence en " + duracion.toDays() + " días.";
	}

	private static String normalizar(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}
}
