package app.meucarrinho.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * Fails startup when a port has zero or two beans, so a misconfigured swap cannot silently fall back (spec §5,
 * ADR 0007). It runs on bean definitions, before any adapter connects to anything. Every output port may have at
 * most one bean; the ports the wired use cases need must have exactly one.
 */
final class PortBeanVerifier implements BeanFactoryPostProcessor {
    private static final String APPLICATION = "app.meucarrinho.application";
    private static final String OUTPUT_PORT = "app\\.meucarrinho\\.application\\.[a-z]+\\.port\\.[A-Za-z0-9]+";

    private final Map<Class<?>, String> required;

    /** @param required each port the core cannot start without, mapped to what selects its adapter */
    PortBeanVerifier(Map<Class<?>, String> required) {
        this.required = new LinkedHashMap<>(required);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beans) {
        List<String> problems = new ArrayList<>();
        for (Class<?> port : outputPorts(beans.getBeanClassLoader())) {
            String[] names = beans.getBeanNamesForType(port, true, false);
            if (names.length > 1) {
                problems.add(port.getSimpleName() + " has " + names.length + " beans " + Arrays.toString(names)
                        + "; select exactly one adapter");
            }
        }
        required.forEach((port, selectedBy) -> {
            if (beans.getBeanNamesForType(port, true, false).length == 0) {
                problems.add(port.getSimpleName() + " has no bean; check " + selectedBy);
            }
        });
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Port wiring is wrong:\n  " + String.join("\n  ", problems));
        }
    }

    private static List<Class<?>> outputPorts(ClassLoader classLoader) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                AnnotationMetadata metadata = definition.getMetadata();
                return metadata.isInterface() && !metadata.isAnnotation() && metadata.isIndependent();
            }
        };
        scanner.addIncludeFilter((reader, factory) -> reader.getClassMetadata().getClassName().matches(OUTPUT_PORT));
        return scanner.findCandidateComponents(APPLICATION).stream()
                .<Class<?>>map(definition -> ClassUtils.resolveClassName(definition.getBeanClassName(), classLoader))
                .filter(type -> !type.isSealed())
                .toList();
    }
}
