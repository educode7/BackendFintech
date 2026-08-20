package com.wallet.account;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

class AccountArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.wallet.account");

    @Test
    @DisplayName("Domain no depende de Infrastructure")
    void domain_noDependeDeInfraestructura() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Controllers solo residen en infrastructure.web")
    void controllers_soloEnWeb() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..infrastructure.web..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("@RestController solo se aplica a clases del paquete web")
    void restController_soloEnWeb() {
        ArchRule rule = noClasses()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideOutsideOfPackage("..infrastructure.web..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Sin inyección por campo")
    void sinFieldInjection() {
        ArchRule rule = noFields()
            .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class);

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Dinero nunca se representa con float ni double")
    void dineroEsBigDecimal() {
        ArchRule rule = noFields()
            .that().haveName("amount").or().haveName("balance").or().haveName("money")
            .should().haveRawType("float")
            .orShould().haveRawType("double");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Excepciones de negocio viven en domain")
    void excepcionesNegocioEnDominio() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Exception")
            .should().resideInAPackage("..domain..")
            .orShould().resideInAPackage("..infrastructure.web.exception..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("El aggregate Account vive en domain")
    void accountEnDomain() {
        ArchRule rule = classes()
            .that().haveSimpleName("Account")
            .should().resideInAPackage("..domain..");

        rule.check(CLASSES);
    }
}
