package com.wallet.shared;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class SharedArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.wallet.shared");

    @Test
    @DisplayName("Shared no depende de Spring Boot")
    void shared_noDependeDeSpringBoot() {
        ArchRule rule = noClasses()
            .should().dependOnClassesThat().resideInAPackage("org.springframework.boot..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Shared no depende de org.springframework (ni boot ni core)")
    void shared_noDependeDeSpringCore() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage("com.wallet.shared.security")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Money nunca usa float ni double")
    void money_sinFloatNiDouble() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameStartingWith("Money")
            .should().dependOnClassesThat().haveFullyQualifiedName("java.lang.Float")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.lang.Double");

        rule.check(CLASSES);
    }
}
