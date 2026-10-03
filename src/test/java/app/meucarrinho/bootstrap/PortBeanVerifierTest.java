package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.lists.ListService;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.notifications.port.EmailSender;
import app.meucarrinho.testfixtures.lists.InMemoryShoppingListRepository;
import app.meucarrinho.testfixtures.notifications.RecordingEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;

class PortBeanVerifierTest {
    private final ApplicationContextRunner core = new ApplicationContextRunner()
            .withUserConfiguration(CoreConfiguration.class);

    @Test
    void the_core_starts_when_each_port_it_needs_has_one_adapter_and_unused_ports_have_none() {
        core.withUserConfiguration(InMemoryPortsConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(ListService.class)
                        .doesNotHaveBean(EmailSender.class));
    }

    @Test
    void startup_fails_when_a_port_the_core_needs_has_no_adapter() {
        core.run(context -> assertThat(failure(context))
                .contains("ShoppingListRepository has no bean; check carrinho.adapters.persistence")
                .contains("UnitOfWork has no bean; check carrinho.adapters.persistence"));
    }

    @Test
    void startup_fails_when_a_port_has_two_adapters() {
        core.withUserConfiguration(InMemoryPortsConfiguration.class, SecondListRepository.class)
                .run(context -> assertThat(failure(context)).contains(
                        "ShoppingListRepository has 2 beans [inMemoryShoppingListRepository, secondListRepository]"));
    }

    @Test
    void two_adapters_fail_even_for_a_port_no_use_case_needs_yet() {
        core.withUserConfiguration(InMemoryPortsConfiguration.class, TwoEmailSenders.class)
                .run(context -> assertThat(failure(context)).contains("EmailSender has 2 beans"));
    }

    private static String failure(AssertableApplicationContext context) {
        assertThat(context).hasFailed();
        return NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure()).getMessage();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecondListRepository {
        @Bean
        ShoppingListRepository secondListRepository() {
            return new InMemoryShoppingListRepository();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TwoEmailSenders {
        @Bean
        EmailSender smtpEmailSender() {
            return new RecordingEmailSender();
        }

        @Bean
        EmailSender resendEmailSender() {
            return new RecordingEmailSender();
        }
    }
}
