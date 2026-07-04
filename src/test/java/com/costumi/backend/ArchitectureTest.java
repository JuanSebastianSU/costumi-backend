package com.costumi.backend;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Control anti-erosión de arquitectura (CLAUDE.md §"Reglas de arquitectura", spec §5.3).
 *
 * <p>Estas reglas <b>fallan el build</b> si la arquitectura se descuadra. Mientras no
 * exista código de módulos, las reglas pasan de forma trivial (paquetes vacíos), pero
 * quedan armadas para morder en cuanto aterrice el primer módulo.
 *
 * <p>Convención de paquetes por módulo (idéntica en todos):
 * {@code com.costumi.backend.<modulo>.{dominio, aplicacion, adaptadores.entrada, adaptadores.salida}}.
 */
@AnalyzeClasses(packages = "com.costumi.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	/** El dominio de un módulo hexagonal NO conoce el framework: cero Spring/JPA/web/JSON. */
	@ArchTest
	static final ArchRule dominio_sin_framework =
			noClasses().that().resideInAPackage("..dominio..")
					.should().dependOnClassesThat().resideInAnyPackage(
							"org.springframework..",
							"jakarta.persistence..",
							"jakarta.servlet..",
							"org.hibernate..",
							"com.fasterxml.jackson..")
					.allowEmptyShould(true)
					.because("el dominio debe ser puro y testeable sin BD ni Spring (CLAUDE.md)");

	/** Las dependencias apuntan hacia adentro: adaptadores -> aplicacion -> dominio. Nunca al revés. */
	@ArchTest
	static final ArchRule dependencias_hacia_adentro =
			layeredArchitecture().consideringOnlyDependenciesInLayers()
					.layer("Dominio").definedBy("..dominio..")
					.layer("Aplicacion").definedBy("..aplicacion..")
					.layer("AdaptadoresEntrada").definedBy("..adaptadores.entrada..")
					.layer("AdaptadoresSalida").definedBy("..adaptadores.salida..")
					.withOptionalLayers(true)
					.whereLayer("AdaptadoresEntrada").mayNotBeAccessedByAnyLayer()
					.whereLayer("AdaptadoresSalida").mayNotBeAccessedByAnyLayer()
					.whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("AdaptadoresEntrada", "AdaptadoresSalida")
					.whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "AdaptadoresEntrada", "AdaptadoresSalida")
					.as("adaptadores -> aplicacion -> dominio (las dependencias apuntan hacia adentro)");
}
