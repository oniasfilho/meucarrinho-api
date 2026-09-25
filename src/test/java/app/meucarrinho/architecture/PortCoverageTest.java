package app.meucarrinho.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PortCoverageTest {
    private static final String PORTS = "app.meucarrinho.application";
    private static final String FIXTURES = "app.meucarrinho.testfixtures";
    private static final String ADAPTERS = "app.meucarrinho.adapter";

    private static final Map<String, String> CONTRACT_PENDING = Map.ofEntries(
            Map.entry("Clock", "trivial"),
            Map.entry("IdGenerator", "trivial"),
            Map.entry("UnitOfWork", "session 2, with the Postgres adapter"),
            Map.entry("DomainEventPublisher", "session 3, with the outbox listeners"),
            Map.entry("ListQueries", "session 2, with the Postgres adapter"),
            Map.entry("IdentityDirectory", "session 2, with the Auth0 adapter"),
            Map.entry("Tracing", "session 2, with the OTel adapter"),
            Map.entry("PhotoStorage", "session 3, with the S3 adapter"),
            Map.entry("EmailSender", "session 3, with the SMTP adapter"),
            Map.entry("PushSender", "session 3, with the log adapter"),
            Map.entry("ListChangeBroadcaster", "session 3, with the STOMP adapter"),
            Map.entry("PresenceTracker", "session 3, with the Redis adapter"),
            Map.entry("FeatureFlags", "session 4, with the PostHog adapter"),
            Map.entry("ProductAnalytics", "session 4, with the PostHog adapter"),
            Map.entry("BusinessMetrics", "session 4, with the OTel adapter"),
            Map.entry("AuditTrail", "session 4, with the audit table"),
            Map.entry("PaymentGateway", "when a billing adapter exists"),
            Map.entry("BillingEventSource", "when a billing adapter exists"));

    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("app.meucarrinho");

    private static Set<JavaClass> outputPorts() {
        return CLASSES.stream()
                .filter(c -> c.getPackageName().startsWith(PORTS) && c.getPackageName().endsWith(".port"))
                .filter(JavaClass::isInterface)
                .filter(c -> !c.isAnnotation() && c.getAllMethods().stream().anyMatch(m -> m.getModifiers()
                        .contains(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)))
                .filter(c -> !c.getSimpleName().equals("package-info"))
                .filter(c -> c.getEnclosingClass().isEmpty())
                .filter(c -> !c.isAssignableTo(Enum.class))
                .filter(c -> !isSealedValueFamily(c))
                .collect(Collectors.toSet());
    }

    private static boolean isSealedValueFamily(JavaClass c) {
        return c.reflect().isSealed();
    }

    @Test
    void finds_every_port_from_spec_5() {
        assertThat(outputPorts()).extracting(JavaClass::getSimpleName).containsExactlyInAnyOrder(
                "ShoppingListRepository", "ReceiptRepository", "AccountRepository", "InvitationRepository",
                "EntitlementRepository", "ListQueries", "ChangeLog", "UnitOfWork", "IdentityDirectory",
                "GuestPassStore", "FeatureFlags", "CatalogSource", "EmailSender", "PushSender", "PhotoStorage",
                "ListChangeBroadcaster", "PresenceTracker", "ProductAnalytics", "BusinessMetrics", "Tracing",
                "PaymentGateway", "BillingEventSource", "Clock", "IdGenerator", "DomainEventPublisher", "AuditTrail");
    }

    @Test
    void every_output_port_has_an_in_memory_fake() {
        Set<String> missing = new TreeSet<>();
        for (JavaClass port : outputPorts()) {
            boolean faked = CLASSES.stream().anyMatch(c -> c.getPackageName().startsWith(FIXTURES)
                    && !c.isInterface() && !c.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                    && c.isAssignableTo(port.getName()));
            if (!faked) {
                missing.add(port.getSimpleName());
            }
        }
        assertThat(missing).as("ports without a fake in testFixtures").isEmpty();
    }

    @Test
    void every_output_port_has_a_contract_suite_or_a_dated_exemption() {
        Set<String> contracts = CLASSES.stream()
                .filter(c -> c.getPackageName().startsWith(FIXTURES) && c.getSimpleName().endsWith("Contract"))
                .map(c -> c.getSimpleName().replaceFirst("Contract$", ""))
                .collect(Collectors.toSet());
        Set<String> missing = new TreeSet<>();
        for (JavaClass port : outputPorts()) {
            String name = port.getSimpleName();
            if (!contracts.contains(name) && !CONTRACT_PENDING.containsKey(name)) {
                missing.add(name);
            }
        }
        assertThat(missing).as("ports with neither a contract suite nor an exemption").isEmpty();
        assertThat(CONTRACT_PENDING.keySet()).as("exemptions for ports that already have a contract")
                .doesNotContainAnyElementsOf(contracts);
    }

    @Test
    void every_adapter_package_runs_the_contract_of_the_port_it_implements() {
        Set<String> missing = new TreeSet<>();
        for (JavaClass adapter : CLASSES.that(com.tngtech.archunit.base.DescribedPredicate.describe("adapters",
                c -> c.getPackageName().startsWith(ADAPTERS + ".") && !c.isInterface()))) {
            for (JavaClass port : outputPorts()) {
                if (!adapter.isAssignableTo(port.getName())) {
                    continue;
                }
                String contract = port.getSimpleName() + "Contract";
                boolean covered = CLASSES.stream().anyMatch(test -> test.getPackageName().equals(adapter.getPackageName())
                        && test.getAllRawSuperclasses().stream().anyMatch(s -> s.getSimpleName().equals(contract)));
                if (!covered && !CONTRACT_PENDING.containsKey(port.getSimpleName())) {
                    missing.add(adapter.getName() + " needs a test extending " + contract);
                }
            }
        }
        assertThat(missing).isEmpty();
    }
}
