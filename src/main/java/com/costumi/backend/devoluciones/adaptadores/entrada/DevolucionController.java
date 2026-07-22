package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.devoluciones.aplicacion.ConsultarDevoluciones;
import com.costumi.backend.devoluciones.aplicacion.RegistrarDevolucion;
import com.costumi.backend.devoluciones.aplicacion.RegistrarDevolucionComando;
import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.devoluciones.dominio.PiezaRevisada;
import com.costumi.backend.inventario.ConsultaDeInventario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Devoluciones de renta (RF-5): checklist + liquidación del depósito, acotado al tenant. */
@RestController
@RequestMapping("/api/v1/devoluciones")
class DevolucionController {

	private final RegistrarDevolucion registrarDevolucion;
	private final ConsultarDevoluciones consultarDevoluciones;
	private final ConsultaDeInventario inventario;
	private final ContextoDeTenant tenant;

	DevolucionController(RegistrarDevolucion registrarDevolucion, ConsultarDevoluciones consultarDevoluciones,
			ConsultaDeInventario inventario, ContextoDeTenant tenant) {
		this.registrarDevolucion = registrarDevolucion;
		this.consultarDevoluciones = consultarDevoluciones;
		this.inventario = inventario;
		this.tenant = tenant;
	}

	/** Respuesta con piezas enriquecidas (nombre + foto) resolviendo cada prendaId contra el inventario. */
	private DevolucionResponse resp(UUID empresaId, Devolucion d) {
		List<UUID> prendaIds = d.piezas().stream().map(p -> p.prendaId()).toList();
		return DevolucionResponse.desde(d, inventario.resumenDePrendas(empresaId, prendaIds));
	}

	@PostMapping
	ResponseEntity<DevolucionResponse> registrar(@Valid @RequestBody RegistrarDevolucionRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = tenant.empresaIdRequerida();
		List<PiezaRevisada> piezas = (request.piezas() == null) ? List.of()
				: request.piezas().stream()
						.map(p -> PiezaRevisada.de(p.prendaId(), p.descripcion(), p.llego(), p.estado(),
								p.perdidaCobrada()))
						.toList();
		java.time.LocalDate fechaReal = request.fechaDevolucionReal() != null ? request.fechaDevolucionReal()
				: java.time.LocalDate.now();
		Devolucion devolucion = registrarDevolucion.ejecutar(new RegistrarDevolucionComando(empresaId,
				request.rentaId(), request.deposito(), request.cargoPorDanos(), request.cargoPorRetraso(),
				fechaReal, piezas));
		URI location = uriBuilder.path("/api/v1/devoluciones/{id}").buildAndExpand(devolucion.id()).toUri();
		return ResponseEntity.created(location).body(resp(empresaId, devolucion));
	}

	/** Página de devoluciones. {@code buscar} filtra por la descripción escrita en una pieza revisada. */
	@GetMapping
	com.costumi.backend.compartido.RespuestaPaginada<DevolucionResponse> listar(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String buscar,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer pagina,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer tamano,
			@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		if (empresaId == null) {
			return new com.costumi.backend.compartido.RespuestaPaginada<>(List.of(), 0, 0, 0, 0);
		}
		UUID empresa = UUID.fromString(empresaId);
		return com.costumi.backend.compartido.RespuestaPaginada.desde(
				consultarDevoluciones.deEmpresa(empresa, buscar, com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano)),
				d -> resp(empresa, d));
	}
}
