package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.ConsultaDePermisos;
import com.costumi.backend.identidad.dominio.Permiso;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;

/**
 * Aplica los permisos granulares por empleado (RF-1.5): si el dueño desactivó la casilla de la sección
 * y acción del request para ese empleado, responde 403. Sin override, no interfiere (la autorización por
 * rol la resuelve {@code SecurityConfig}). El Dueño y el SuperAdmin no son restringibles.
 */
@Component
class InterceptorDePermisos implements HandlerInterceptor {

	private final ConsultaDePermisos permisos;

	InterceptorDePermisos(ConsultaDePermisos permisos) {
		this.permisos = permisos;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof JwtAuthenticationToken token)) {
			return true; // no autenticado por JWT: lo maneja SecurityConfig
		}
		Jwt jwt = token.getToken();
		String rol = jwt.getClaimAsString("rol");
		if (rol == null || "SUPERADMIN".equals(rol) || "DUENO".equals(rol)) {
			return true; // plataforma y dueño no son restringibles
		}
		String subject = jwt.getSubject();
		if (subject == null) {
			return true;
		}
		Optional<Permiso> requerido = MapaDeSecciones.permisoRequerido(request.getMethod(),
				rutaSinContexto(request));
		if (requerido.isEmpty()) {
			return true; // ruta no sujeta a permiso granular
		}
		Permiso permiso = requerido.get();
		if (permisos.bloqueado(UUID.fromString(subject), permiso.seccion(), permiso.accion())) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		return true;
	}

	private static String rutaSinContexto(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String contexto = request.getContextPath();
		return (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto))
				? uri.substring(contexto.length())
				: uri;
	}
}
