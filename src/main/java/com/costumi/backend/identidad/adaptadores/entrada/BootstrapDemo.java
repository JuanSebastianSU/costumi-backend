package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de demo/prueba al inicio: crea una <b>Empresa ACTIVA</b> y su <b>DUEÑO</b> (usuario que puede
 * operar toda la empresa) si se configuran email+password y aún no existen. Idempotente y apagado por
 * defecto; en el despliegue se setea por entorno (COSTUMI_DEMO_DUENO_EMAIL / COSTUMI_DEMO_DUENO_PASSWORD).
 * Cubre el hueco de que hoy no hay API para provisionar el primer dueño de una empresa.
 */
@Component
@Order(20)
class BootstrapDemo implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapDemo.class);

	private final EmpresaRepository empresas;
	private final SucursalRepository sucursales;
	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;
	private final String nombreEmpresa;

	BootstrapDemo(EmpresaRepository empresas, SucursalRepository sucursales, UsuarioRepository usuarios,
			PasswordEncoder passwordEncoder,
			@Value("${costumi.demo.dueno.email:}") String email,
			@Value("${costumi.demo.dueno.password:}") String password,
			@Value("${costumi.demo.empresa.nombre:Costumi Demo}") String nombreEmpresa) {
		this.empresas = empresas;
		this.sucursales = sucursales;
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
		this.nombreEmpresa = nombreEmpresa;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank()) {
			return; // no configurado: no se siembra
		}
		if (usuarios.buscarPorEmail(email).isPresent()) {
			return; // ya existe (idempotente)
		}
		Empresa empresa = Empresa.registrar(nombreEmpresa);
		empresa.aprobar(); // PENDIENTE -> ACTIVA (puede operar)
		empresa = empresas.guardar(empresa);
		// Casa matriz: sin al menos una sucursal, la empresa no puede operar rentas/ventas (necesitan sucursalId).
		Sucursal casaMatriz = sucursales.guardar(Sucursal.crear(empresa.id(), "Casa Matriz", null));
		usuarios.guardar(Usuario.crear(empresa.id(), email, passwordEncoder.encode(password), Rol.DUENO));
		log.info("Empresa demo ACTIVA '{}' (sucursal '{}') y dueño de bootstrap creados: {}",
				nombreEmpresa, casaMatriz.id(), email);
	}
}
