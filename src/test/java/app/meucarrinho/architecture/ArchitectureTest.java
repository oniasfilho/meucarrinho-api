package app.meucarrinho.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;

@AnalyzeClasses(packages = "app.meucarrinho", importOptions = ArchitectureTest.MainClassesOnly.class)
class ArchitectureTest {
    private static final String ROOT = "app.meucarrinho";
    private static final String APPLICATION = ROOT + ".application.";

    static final class MainClassesOnly implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/classes/java/test/")
                    && !location.contains("/classes/java/testFixtures/")
                    && !location.contains("/classes/java/integrationTest/")
                    && !location.contains("-test-fixtures.jar");
        }
    }

    @ArchTest
    static final ArchRule domain_depends_only_on_the_jdk_and_jspecify = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java..", "org.jspecify..")
            .because("the domain is plain Java (spec §3)");

    @ArchTest
    static final ArchRule domain_does_not_use_jdbc = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..");

    @ArchTest
    static final ArchRule application_imports_no_framework_or_vendor = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "io.micrometer..",
                    "io.opentelemetry..",
                    "com.stripe..",
                    "com.posthog..",
                    "com.auth0..",
                    "software.amazon..",
                    "java.sql..",
                    "javax.sql..",
                    "com.mongodb..",
                    "org.bson..")
            .because("use cases and ports name no framework or vendor (spec §11)");

    @ArchTest
    static final ArchRule core_does_not_depend_on_the_edges = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage("app.meucarrinho.api..", "app.meucarrinho.adapter..", "app.meucarrinho.bootstrap..");

    @ArchTest
    static final ArchRule api_depends_only_on_input_ports = noClasses()
            .that().resideInAPackage("app.meucarrinho.api..")
            .should().dependOnClassesThat(outputPortsUseCaseClassesOrAdapters())
            .allowEmptyShould(true)
            .because("controllers depend on input-port interfaces only (spec §3, §11)");

    @ArchTest
    static final ArchRule api_never_sees_persistence_types = noClasses()
            .that().resideInAPackage("app.meucarrinho.api..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "org.springframework.dao..")
            .allowEmptyShould(true)
            .because("nothing from SQL reaches a response (spec §13)");

    @ArchTest
    static final ArchRule adapters_do_not_depend_on_each_other = slices()
            .matching(ROOT + ".adapter.(*).(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_do_not_depend_on_api_or_bootstrap = noClasses()
            .that().resideInAPackage("app.meucarrinho.adapter..")
            .should().dependOnClassesThat().resideInAnyPackage("app.meucarrinho.api..", "app.meucarrinho.bootstrap..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule only_bootstrap_references_adapters = noClasses()
            .that().resideOutsideOfPackages("app.meucarrinho.adapter..", "app.meucarrinho.bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("app.meucarrinho.adapter..")
            .because("only bootstrap may reference more than one adapter (spec §11)");

    @ArchTest
    static final ArchRule adapter_classes_are_package_private_except_configuration = classes()
            .that().resideInAPackage(ROOT + ".adapter.*.*..")
            .and().areNotAnnotatedWith("org.springframework.context.annotation.Configuration")
            .and().doNotHaveSimpleName("package-info")
            .should().notBePublic()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule stripe_only_in_its_adapter = vendorOnlyIn("com.stripe..", "..adapter.billing.stripe..");

    @ArchTest
    static final ArchRule auth0_only_in_its_adapter = vendorOnlyIn("com.auth0..", "..adapter.identity.auth0..");

    @ArchTest
    static final ArchRule aws_only_in_the_storage_adapter = vendorOnlyIn("software.amazon..", "..adapter.storage.s3..");

    @ArchTest
    static final ArchRule mongo_only_in_its_adapter = vendorOnlyIn("com.mongodb..", "..adapter.persistence.mongo..");

    @ArchTest
    static final ArchRule bson_only_in_the_mongo_adapter = vendorOnlyIn("org.bson..", "..adapter.persistence.mongo..");

    @ArchTest
    static final ArchRule posthog_only_in_its_adapters = vendorOnlyIn(
            "com.posthog..", "..adapter.telemetry.posthog..", "..adapter.flags.posthog..");

    @ArchTest
    static final ArchRule opentelemetry_only_in_its_adapter = vendorOnlyIn(
            "io.opentelemetry..", "..adapter.telemetry.otel..", "app.meucarrinho.bootstrap..");

    @ArchTest
    static final ArchRule capabilities_meet_only_through_their_api = classes()
            .that().resideInAPackage("..application..")
            .should(reachOtherCapabilitiesOnlyThroughTheirApi())
            .because("capabilities talk through their api subpackage or domain events (spec §3)");

    @ArchTest
    static final ArchRule capabilities_are_free_of_cycles = slices()
            .matching(ROOT + ".application.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule cart_writes_never_call_side_effect_ports = noClasses()
            .that().resideInAnyPackage("..application.lists..", "..application.sync..")
            .should().dependOnClassesThat(simpleName("EmailSender")
                    .or(simpleName("PushSender"))
                    .or(simpleName("PaymentGateway")))
            .because("e-mail, push and billing run from event listeners, never inside a cart write (spec §13)");

    @ArchTest
    static final ArchRule main_class_lives_in_bootstrap = classes()
            .that().areAnnotatedWith("org.springframework.boot.autoconfigure.SpringBootApplication")
            .should().resideInAPackage("app.meucarrinho.bootstrap..");

    private static ArchRule vendorOnlyIn(String vendorPackage, String... allowedPackages) {
        return noClasses()
                .that().resideOutsideOfPackages(allowedPackages)
                .should().dependOnClassesThat().resideInAPackage(vendorPackage)
                .because("each vendor SDK is used only inside its own adapter package (spec §11)");
    }

    private static DescribedPredicate<JavaClass> outputPortsUseCaseClassesOrAdapters() {
        DescribedPredicate<JavaClass> useCaseClass =
                resideInAPackage("..application..").and(simpleNameEndingWith("Service"));
        return resideInAnyPackage("..application..port..", "app.meucarrinho.adapter..", "app.meucarrinho.bootstrap..")
                .or(useCaseClass)
                .as("output ports, use-case classes or adapters");
    }

    private static ArchCondition<JavaClass> reachOtherCapabilitiesOnlyThroughTheirApi() {
        return new ArchCondition<>("reach other capabilities only through their api subpackage") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> from = capabilityOf(origin);
                if (from.isEmpty()) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    Optional<String> to = capabilityOf(target);
                    if (to.isEmpty() || to.equals(from) || isShared(target, to.get())) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                }
            }
        };
    }

    private static boolean isShared(JavaClass target, String capability) {
        String pkg = target.getPackageName();
        return capability.equals("common")
                || pkg.equals(APPLICATION + capability + ".api")
                || pkg.startsWith(APPLICATION + capability + ".api.")
                || pkg.equals(APPLICATION + "telemetry.port");
    }

    private static Optional<String> capabilityOf(JavaClass javaClass) {
        String pkg = javaClass.getPackageName();
        if (!pkg.startsWith(APPLICATION)) {
            return Optional.empty();
        }
        String rest = pkg.substring(APPLICATION.length());
        int dot = rest.indexOf('.');
        return Optional.of(dot < 0 ? rest : rest.substring(0, dot));
    }
}
