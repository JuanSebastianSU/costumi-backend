package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.identidad.aplicacion.AsignarSucursales;
import com.costumi.backend.identidad.aplicacion.CambiarRolDeEmpleado;
import com.costumi.backend.identidad.aplicacion.GestionarEstadoDeEmpleado;
import com.costumi.backend.identidad.aplicacion.GestionarInvitaciones;
import com.costumi.backend.identidad.aplicacion.GestionarMembresiaDeEmpleado;
import com.costumi.backend.identidad.aplicacion.ListarEmpleados;
import com.costumi.backend.identidad.dominio.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Empleados de una empresa (RF-8), acotados al tenant del token. El alta es por <b>invitación</b> (Fase B,
 * paso 3): no se crea una cuenta suelta; se invita por email y la persona acepta (con T&C). También gestiona
 * las sucursales asignadas y la desvinculación (suspender/reactivar/quitar).
 */
@RestController
@RequestMapping("/api/v1/empleados")
class EmpleadoController {

	private final GestionarInvitaciones invitaciones;
	private final AsignarSucursales asignarSucursales;
	private final GestionarEstadoDeEmpleado gestionarEstadoDeEmpleado;
	private final GestionarMembresiaDeEmpleado gestionarMembresia;
	private final ListarEmpleados listarEmpleados;
	private final CambiarRolDeEmpleado cambiarRolDeEmpleado;
	private final ContextoDeTenant tenant;

	EmpleadoController(GestionarInvitaciones invitaciones, AsignarSucursales asignarSucursales,
			GestionarEstadoDeEmpleado gestionarEstadoDeEmpleado, GestionarMembresiaDeEmpleado gestionarMembresia,
			ListarEmpleados listarEmpleados, CambiarRolDeEmpleado cambiarRolDeEmpleado, ContextoDeTenant tenant) {
		this.invitaciones = invitaciones;
		this.asignarSucursales = asignarSucursales;
		this.gestionarEstadoDeEmpleado = gestionarEstadoDeEmpleado;
		this.gestionarMembresia = gestionarMembresia;
		this.listarEmpleados = listarEmpleados;
		this.cambiarRolDeEmpleado = cambiarRolDeEmpleado;
		this.tenant = tenant;
	}

	/**
	 * Lista el personal de la empresa (RF-8, G1). Con la guarda de pirámide (B3): el actor solo ve a quienes
	 * puede gestionar (estrictamente por debajo suyo). DUENO/ENCARGADO.
	 */
	@GetMapping
	com.costumi.backend.compartido.RespuestaPaginada<EmpleadoDetalleResponse> listar(
			@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Integer pagina,
			@RequestParam(required = false) Integer tamano,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return com.costumi.backend.compartido.RespuestaPaginada.desde(
				listarEmpleados.delTenant(empresaId, actorRol(jwt), buscar,
						com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano)),
				EmpleadoDetalleResponse::desde);
	}

	/**
	 * Invita a una persona (por email) a trabajar en la empresa con un rol y sus sucursales (alta = invitación,
	 * Fase B, paso 3). Devuelve el enlace de aceptación (para compartir aunque no haya email real). DUENO/ENCARGADO.
	 */
	@PostMapping
	ResponseEntity<InvitacionResponse> invitar(@Valid @RequestBody InvitarEmpleadoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		UUID actorId = UUID.fromString(jwt.getSubject());
		GestionarInvitaciones.InvitacionCreada creada = invitaciones.invitar(new GestionarInvitaciones.InvitarComando(
				empresaId, actorRol(jwt), actorId, request.email(), request.rol(),
				request.sucursalIds() == null ? Set.of() : Set.copyOf(request.sucursalIds())));
		URI location = uriBuilder.path("/api/v1/empleados/invitaciones/{id}").buildAndExpand(creada.id()).toUri();
		return ResponseEntity.created(location)
				.body(new InvitacionResponse(creada.id(), creada.email(), creada.rol().name(), creada.enlace()));
	}

	/** Las invitaciones pendientes de la empresa. DUENO/ENCARGADO. */
	@GetMapping("/invitaciones")
	List<InvitacionPendienteResponse> invitacionesPendientes() {
		UUID empresaId = tenant.empresaIdRequerida();
		return invitaciones.pendientes(empresaId).stream()
				.map(i -> new InvitacionPendienteResponse(i.id(), i.email(), i.rol().name(), i.expiraEn()))
				.toList();
	}

	/** Cancela una invitación pendiente. DUENO/ENCARGADO. */
	@DeleteMapping("/invitaciones/{invitacionId}")
	ResponseEntity<Void> cancelarInvitacion(@PathVariable UUID invitacionId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		invitaciones.cancelar(empresaId, actorRol(jwt), invitacionId);
		return ResponseEntity.noContent().build();
	}

	/** Da de baja la CUENTA del empleado (no podrá ni autenticarse). DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/desactivar")
	EmpleadoResponse desactivar(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		UUID actorId = UUID.fromString(jwt.getSubject());
		return EmpleadoResponse.desde(gestionarEstadoDeEmpleado.desactivar(empresaId, actorRol(jwt), actorId, usuarioId));
	}

	/** Reactiva una CUENTA dada de baja. DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/activar")
	EmpleadoResponse activar(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return EmpleadoResponse.desde(gestionarEstadoDeEmpleado.activar(empresaId, actorRol(jwt), usuarioId));
	}

	/** Suspende la membresía de trabajo (reversible): el empleado queda solo-cliente. DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/suspender")
	MembresiaEstadoResponse suspender(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return MembresiaEstadoResponse.desde(gestionarMembresia.suspender(empresaId, actorRol(jwt), usuarioId));
	}

	/** Reactiva una membresía suspendida. DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/reactivar")
	MembresiaEstadoResponse reactivar(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return MembresiaEstadoResponse.desde(gestionarMembresia.reactivar(empresaId, actorRol(jwt), usuarioId));
	}

	/** Da de baja al empleado (despido): definitivo, se vuelve re-invitando. Queda solo-cliente. DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/quitar")
	MembresiaEstadoResponse quitar(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return MembresiaEstadoResponse.desde(gestionarMembresia.quitar(empresaId, actorRol(jwt), usuarioId));
	}

	/** Asigna el empleado a una o varias sucursales (RF-1.2/8.1), con la pirámide de sucursales (paso 4). */
	@PutMapping("/{usuarioId}/sucursales")
	List<UUID> asignarSucursales(@PathVariable UUID usuarioId, @Valid @RequestBody AsignarSucursalesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		UUID actorId = UUID.fromString(jwt.getSubject());
		asignarSucursales.asignar(empresaId, actorRol(jwt), actorId, usuarioId, Set.copyOf(request.sucursalIds()));
		return asignarSucursales.sucursalesDe(empresaId, actorRol(jwt), usuarioId);
	}

	@GetMapping("/{usuarioId}/sucursales")
	List<UUID> sucursalesDe(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return asignarSucursales.sucursalesDe(empresaId, actorRol(jwt), usuarioId);
	}

	/**
	 * Cambia el rol de un empleado ya activo (RF-8, G2). Con la guarda de pirámide (B3): solo se puede fijar
	 * un rol estrictamente por debajo del propio y sobre un empleado también por debajo. DUENO/ENCARGADO.
	 */
	@PutMapping("/{usuarioId}/rol")
	EmpleadoResponse cambiarRol(@PathVariable UUID usuarioId, @Valid @RequestBody CambiarRolRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		return EmpleadoResponse.desde(
				cambiarRolDeEmpleado.ejecutar(empresaId, actorRol(jwt), usuarioId, request.rol()));
	}

	private static Rol actorRol(Jwt jwt) {
		return Rol.valueOf(jwt.getClaimAsString("rol"));
	}

	record InvitarEmpleadoRequest(@NotBlank @Email String email, @NotNull Rol rol, List<UUID> sucursalIds) {
	}

	record InvitacionResponse(UUID id, String email, String rol, String enlace) {
	}

	record InvitacionPendienteResponse(UUID id, String email, String rol, Instant expiraEn) {
	}

	record CambiarRolRequest(@NotNull Rol rol) {
	}

	record AsignarSucursalesRequest(@NotNull List<UUID> sucursalIds) {
	}
}
