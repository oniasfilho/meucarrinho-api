package app.meucarrinho.bootstrap;

import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.application.accounts.UpsertAccount;
import app.meucarrinho.application.lists.CreateShoppingList;
import app.meucarrinho.application.lists.FinishPurchase;
import app.meucarrinho.application.lists.PickItem;
import app.meucarrinho.application.lists.QuickAddItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@code carrinho.seed.enabled=true} (set by {@code make run}) loads the demo data on startup; off by default. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("carrinho.seed.enabled")
class SeedConfiguration {
    @Bean
    DemoSeed demoSeed(UpsertAccount upsertAccount, GetAccount getAccount, CreateShoppingList createList,
            QuickAddItem quickAdd, PickItem pick, FinishPurchase finish) {
        return new DemoSeed(upsertAccount, getAccount, createList, quickAdd, pick, finish);
    }
}
