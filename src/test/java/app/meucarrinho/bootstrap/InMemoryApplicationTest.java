package app.meucarrinho.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Boots the real application class on the testFixtures fakes, with {@link TestActor} saying who calls: wiring,
 * adapter selection and the REST layer, no Docker. Without a DataSource the JDBC outbox has nothing to write to, so
 * its auto-configuration is off too; the recording publisher stands in for it. Every test class using this shares
 * one cached context, so tests create their own accounts and lists instead of expecting empty stores.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = MeuCarrinhoApplication.class, properties = {
        "carrinho.adapters.persistence=memory",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration,"
                + "org.springframework.modulith.events.config.EventPublicationAutoConfiguration"})
@Import({InMemoryPortsConfiguration.class, TestActor.Configuration.class})
public @interface InMemoryApplicationTest {}
