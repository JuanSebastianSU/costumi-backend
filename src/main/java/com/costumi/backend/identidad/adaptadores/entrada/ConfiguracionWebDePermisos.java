package com.costumi.backend.identidad.adaptadores.entrada;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor de permisos granulares (RF-1.5) para las rutas del API. */
@Configuration
class ConfiguracionWebDePermisos implements WebMvcConfigurer {

	private final InterceptorDePermisos interceptorDePermisos;

	ConfiguracionWebDePermisos(InterceptorDePermisos interceptorDePermisos) {
		this.interceptorDePermisos = interceptorDePermisos;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(interceptorDePermisos).addPathPatterns("/api/v1/**");
	}
}
