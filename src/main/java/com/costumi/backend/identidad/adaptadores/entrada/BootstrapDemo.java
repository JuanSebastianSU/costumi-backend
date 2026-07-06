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

import java.util.UUID;

/**
 * Datos de demo/prueba al inicio: crea una <b>Empresa ACTIVA</b> con su <b>Casa Matriz</b> y su
 * <b>DUEÑO</b> (opera toda la empresa), y opcionalmente una cuenta de <b>MOSTRADOR</b> para la app del
 * cliente (mismo login, el rol lo define la cuenta). Todo idempotente y apagado por defecto; en el
 * despliegue se setea por entorno (COSTUMI_DEMO_DUENO_* y COSTUMI_DEMO_CLIENTE_*).
 * Cubre el hueco de que hoy no hay API para provisionar el primer usuario de una empresa.
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
	private final String clienteEmail;
	private final String clientePassword;

	BootstrapDemo(EmpresaRepository empresas, SucursalRepository sucursales, UsuarioRepository usuarios,
			PasswordEncoder passwordEncoder,
			@Value("${costumi.demo.dueno.email:}") String email,
			@Value("${costumi.demo.dueno.password:}") String password,
			@Value("${costumi.demo.empresa.nombre:Costumi Demo}") String nombreEmpresa,
			@Value("${costumi.demo.cliente.email:}") String clienteEmail,
			@Value("${costumi.demo.cliente.password:}") String clientePassword) {
		this.empresas = empresas;
		this.sucursales = sucursales;
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
		this.nombreEmpresa = nombreEmpresa;
		this.clienteEmail = clienteEmail;
		this.clientePassword = clientePassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		UUID empresaId = sembrarDueno();
		if (empresaId != null) {
			sembrarCliente(empresaId);
		}
	}

	/**
	 * Asegura la empresa demo ACTIVA + Casa Matriz + dueño. Devuelve el id de la empresa demo
	 * (la existente si el dueño ya estaba, o la recién creada), o {@code null} si no está configurado.
	 */
	private UUID sembrarDueno() {
		if (email.isBlank() || password.isBlank()) {
			return null; // no configurado: no se siembra
		}
		var existente = usuarios.buscarPorEmail(email);
		if (existente.isPresent()) {
			return existente.get().empresaId(); // ya existe (idempotente): reusamos su empresa
		}
		Empresa empresa = Empresa.registrar(nombreEmpresa);
		empresa.aprobar(); // PENDIENTE -> ACTIVA (puede operar)
		empresa = empresas.guardar(empresa);
		// Casa matriz: sin al menos una sucursal, la empresa no puede operar rentas/ventas (necesitan sucursalId).
		Sucursal casaMatriz = sucursales.guardar(Sucursal.crear(empresa.id(), "Casa Matriz", null));
		usuarios.guardar(Usuario.crear(empresa.id(), email, passwordEncoder.encode(password), Rol.DUENO));
		log.info("Empresa demo ACTIVA '{}' (sucursal '{}') y dueño de bootstrap creados: {}",
				nombreEmpresa, casaMatriz.id(), email);
		return empresa.id();
	}

	/**
	 * Asegura una cuenta de MOSTRADOR en la empresa demo para la app del cliente (el rol define la
	 * experiencia al ingresar). Idempotente y opcional (si no se configura email+password, no hace nada).
	 */
	private void sembrarCliente(UUID empresaId) {
		if (clienteEmail.isBlank() || clientePassword.isBlank()) {
			return; // no configurado
		}
		if (usuarios.buscarPorEmail(clienteEmail).isPresent()) {
			return; // ya existe (idempotente)
		}
		usuarios.guardar(Usuario.crear(empresaId, clienteEmail, passwordEncoder.encode(clientePassword), Rol.MOSTRADOR));
		log.info("Cuenta de cliente (MOSTRADOR) de bootstrap creada en la empresa demo: {}", clienteEmail);
	}
}
