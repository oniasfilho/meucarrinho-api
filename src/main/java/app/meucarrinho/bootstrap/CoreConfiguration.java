package app.meucarrinho.bootstrap;

import app.meucarrinho.application.accounts.AccountLookupService;
import app.meucarrinho.application.accounts.AccountService;
import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.ActiveListsService;
import app.meucarrinho.application.lists.FinishPurchaseService;
import app.meucarrinho.application.lists.ItemService;
import app.meucarrinho.application.lists.ListService;
import app.meucarrinho.application.lists.ReceiptReuseService;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.receipts.ReceiptBookService;
import app.meucarrinho.application.receipts.ReceiptQueryService;
import app.meucarrinho.application.receipts.api.ReceiptBook;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires every use case to whichever adapters the {@code carrinho.adapters.*} keys selected (spec §5). Beans are
 * declared by their service type so callers inject them by input-port interface.
 */
@Configuration(proxyBeanMethods = false)
class CoreConfiguration {
    private static final String PERSISTENCE = "carrinho.adapters.persistence";

    @Bean
    static PortBeanVerifier portBeanVerifier() {
        Map<Class<?>, String> required = new LinkedHashMap<>();
        required.put(ShoppingListRepository.class, PERSISTENCE);
        required.put(ReceiptRepository.class, PERSISTENCE);
        required.put(AccountRepository.class, PERSISTENCE);
        required.put(ListQueries.class, PERSISTENCE);
        required.put(UnitOfWork.class, PERSISTENCE);
        required.put(DomainEventPublisher.class, PERSISTENCE);
        required.put(Clock.class, CoreConfiguration.class.getSimpleName());
        required.put(IdGenerator.class, CoreConfiguration.class.getSimpleName());
        return new PortBeanVerifier(required);
    }

    @Bean
    Clock systemClock() {
        return new SystemClock();
    }

    @Bean
    IdGenerator uuid7IdGenerator(Clock clock) {
        return new Uuid7IdGenerator(clock, new SecureRandom());
    }

    @Bean
    ReceiptBookService receiptBookService(ReceiptRepository receipts) {
        return new ReceiptBookService(receipts);
    }

    @Bean
    ReceiptQueryService receiptQueryService(ReceiptRepository receipts) {
        return new ReceiptQueryService(receipts);
    }

    @Bean
    ListService listService(ShoppingListRepository lists, UnitOfWork unitOfWork, DomainEventPublisher events,
            IdGenerator ids, Clock clock) {
        return new ListService(lists, unitOfWork, events, ids, clock);
    }

    @Bean
    ActiveListsService activeListsService(ListQueries queries) {
        return new ActiveListsService(queries);
    }

    @Bean
    ItemService itemService(ShoppingListRepository lists, UnitOfWork unitOfWork, DomainEventPublisher events,
            IdGenerator ids, Clock clock) {
        return new ItemService(lists, unitOfWork, events, ids, clock);
    }

    @Bean
    FinishPurchaseService finishPurchaseService(ShoppingListRepository lists, ReceiptBook receipts,
            UnitOfWork unitOfWork, DomainEventPublisher events, IdGenerator ids, Clock clock) {
        return new FinishPurchaseService(lists, receipts, unitOfWork, events, ids, clock);
    }

    @Bean
    ReceiptReuseService receiptReuseService(ShoppingListRepository lists, ReceiptBook receipts,
            UnitOfWork unitOfWork, DomainEventPublisher events, IdGenerator ids, Clock clock) {
        return new ReceiptReuseService(lists, receipts, unitOfWork, events, ids, clock);
    }

    @Bean
    AccountService accountService(AccountRepository accounts, UnitOfWork unitOfWork, IdGenerator ids,
            Clock clock) {
        return new AccountService(accounts, unitOfWork, ids, clock);
    }

    @Bean
    AccountLookupService accountLookupService(AccountRepository accounts) {
        return new AccountLookupService(accounts);
    }
}
