package com.hufs.capstone.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
		packages = "com.hufs.capstone.backend",
		importOptions = ImportOption.DoNotIncludeTests.class
)
class DomainBoundaryTest {

	@ArchTest
	static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
			.that().resideInAnyPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"com.hufs.capstone.backend..application..",
					"com.hufs.capstone.backend..api..",
					"com.hufs.capstone.backend..infrastructure..",
					"com.hufs.capstone.backend..external.."
			)
			.because("domain code must remain independent of use cases and implementation details");
}
