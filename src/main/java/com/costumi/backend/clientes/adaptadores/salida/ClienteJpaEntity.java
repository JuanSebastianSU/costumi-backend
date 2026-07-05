package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA del Cliente. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "cliente")
@Filter(name = FiltroTenant.NOMBRE)
class ClienteJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Column(length = 40)
	private String telefono;

	@Column(length = 200)
	private String email;

	@Column(length = 60)
	private String documento;

	@Column(length = 300)
	private String direccion;

	@Column(name = "en_lista_negra", nullable = false)
	private boolean enListaNegra;

	protected ClienteJpaEntity() {
		// requerido por JPA
	}

	ClienteJpaEntity(UUID id, UUID empresaId, String nombre, String telefono, String email, String documento,
			String direccion, boolean enListaNegra) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.telefono = telefono;
		this.email = email;
		this.documento = documento;
		this.direccion = direccion;
		this.enListaNegra = enListaNegra;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	String getNombre() {
		return nombre;
	}

	String getTelefono() {
		return telefono;
	}

	String getEmail() {
		return email;
	}

	String getDocumento() {
		return documento;
	}

	String getDireccion() {
		return direccion;
	}

	boolean isEnListaNegra() {
		return enListaNegra;
	}
}
