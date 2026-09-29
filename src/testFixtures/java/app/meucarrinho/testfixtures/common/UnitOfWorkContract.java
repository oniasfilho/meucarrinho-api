package app.meucarrinho.testfixtures.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.application.common.port.UnitOfWork;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public interface UnitOfWorkContract {
    UnitOfWork unitOfWork();

    @Test
    default void returns_the_result_of_work_executed_once() {
        var executions = new AtomicInteger();

        String result = unitOfWork().execute(() -> {
            executions.incrementAndGet();
            return "complete";
        });

        assertThat(result).isEqualTo("complete");
        assertThat(executions).hasValue(1);
    }

    @Test
    default void propagates_work_failures() {
        var failure = new IllegalStateException("work failed");

        assertThatThrownBy(() -> unitOfWork().execute(() -> {
                    throw failure;
                }))
                .isSameAs(failure);
    }
}
