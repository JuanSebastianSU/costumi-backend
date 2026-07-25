package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.compartido.AlmacenDeImagenesPublico;
import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * El Dueño edita la identidad de su tienda y sube su logo/portada, reusando el almacén de imágenes
 * compartido (mismo S3 que las fotos de prenda). La empresa se carga por su id (el del token).
 */
@Service
class IdentidadDeTiendaService implements GestionarIdentidadDeTienda {

	private final EmpresaRepository empresas;
	private final AlmacenDeImagenesPublico imagenes;

	IdentidadDeTiendaService(EmpresaRepository empresas, AlmacenDeImagenesPublico imagenes) {
		this.empresas = empresas;
		this.imagenes = imagenes;
	}

	@Override
	@Transactional
	public Empresa editar(UUID empresaId, String nombre, String ubicacion, String contacto, String descripcion,
			String ciudad) {
		Empresa empresa = cargar(empresaId);
		empresa.editarIdentidad(nombre, ubicacion, contacto, descripcion, ciudad);
		return empresas.guardar(empresa);
	}

	@Override
	@Transactional
	public Empresa asignarLogo(UUID empresaId, byte[] contenido) {
		Empresa empresa = cargar(empresaId);
		empresa.asignarLogo(imagenes.subir(contenido, "empresas/" + empresaId + "/logo/"));
		return empresas.guardar(empresa);
	}

	@Override
	@Transactional
	public Empresa asignarPortada(UUID empresaId, byte[] contenido) {
		Empresa empresa = cargar(empresaId);
		empresa.asignarPortada(imagenes.subir(contenido, "empresas/" + empresaId + "/portada/"));
		return empresas.guardar(empresa);
	}

	private Empresa cargar(UUID empresaId) {
		return empresas.buscarPorId(empresaId).orElseThrow(() -> new EmpresaNoEncontrada(empresaId));
	}
}
