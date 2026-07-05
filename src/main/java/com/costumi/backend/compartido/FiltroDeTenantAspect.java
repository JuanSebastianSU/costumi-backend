package com.costumi.backend.compartido;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fuerza el aislamiento multi-tenant (§5.4): antes de cada llamada a un repositorio JPA activa el
 * filtro Hibernate {@link FiltroTenant#NOMBRE} en la sesión actual con el {@code empresa_id} del token.
 * Así <b>toda</b> consulta queda acotada al tenant, aunque el caso de uso olvide filtrar (defensa en
 * profundidad, no opt-in).
 *
 * <p>Se engancha en el <b>adaptador de repositorio</b> (no en el request) porque OSIV está desactivado:
 * el adaptador siempre se invoca dentro de la transacción del servicio, donde ya hay una sesión viva.
 * Si no hay tenant en el token (p. ej. SuperAdmin de plataforma, o login sin autenticar) no se activa,
 * y esas operaciones ven todo lo que su autorización permita.
 */
@Aspect
@Component
class FiltroDeTenantAspect {

	private final EntityManager entityManager;
	private final ContextoDeTenant tenant;

	FiltroDeTenantAspect(EntityManager entityManager, ContextoDeTenant tenant) {
		this.entityManager = entityManager;
		this.tenant = tenant;
	}

	@Around("execution(public * com.costumi.backend..adaptadores.salida.*RepositoryAdapter.*(..))")
	Object activarFiltroDeTenant(ProceedingJoinPoint punto) throws Throwable {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			tenant.empresaId().ifPresent(empresaId -> entityManager.unwrap(Session.class)
					.enableFilter(FiltroTenant.NOMBRE)
					.setParameter(FiltroTenant.PARAM_EMPRESA, empresaId));
		}
		return punto.proceed();
	}
}
