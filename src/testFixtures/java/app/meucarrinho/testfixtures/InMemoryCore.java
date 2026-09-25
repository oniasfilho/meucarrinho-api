package app.meucarrinho.testfixtures;

import app.meucarrinho.application.accounts.AccountService;
import app.meucarrinho.application.capabilities.CapabilityService;
import app.meucarrinho.application.lists.FinishPurchaseService;
import app.meucarrinho.application.lists.ItemService;
import app.meucarrinho.application.lists.ListService;
import app.meucarrinho.application.lists.ReceiptReuseService;
import app.meucarrinho.application.receipts.ReceiptBookService;
import app.meucarrinho.application.receipts.ReceiptQueryService;
import app.meucarrinho.testfixtures.accounts.InMemoryAccountRepository;
import app.meucarrinho.testfixtures.capabilities.FixedFeatureFlags;
import app.meucarrinho.testfixtures.common.InMemoryUnitOfWork;
import app.meucarrinho.testfixtures.common.MutableClock;
import app.meucarrinho.testfixtures.common.RecordingAuditTrail;
import app.meucarrinho.testfixtures.common.RecordingDomainEventPublisher;
import app.meucarrinho.testfixtures.common.SequentialIdGenerator;
import app.meucarrinho.testfixtures.lists.InMemoryListQueries;
import app.meucarrinho.testfixtures.lists.InMemoryShoppingListRepository;
import app.meucarrinho.testfixtures.notifications.RecordingEmailSender;
import app.meucarrinho.testfixtures.notifications.RecordingPushSender;
import app.meucarrinho.testfixtures.receipts.InMemoryReceiptRepository;
import app.meucarrinho.testfixtures.telemetry.RecordingProductAnalytics;
import java.time.Instant;

public final class InMemoryCore {
    public final MutableClock clock = new MutableClock(Instant.parse("2026-09-26T09:00:00Z"));
    public final SequentialIdGenerator ids = new SequentialIdGenerator();
    public final InMemoryShoppingListRepository listRepository = new InMemoryShoppingListRepository();
    public final InMemoryListQueries listQueries = new InMemoryListQueries(listRepository);
    public final InMemoryReceiptRepository receiptRepository = new InMemoryReceiptRepository();
    public final InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
    public final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    public final RecordingAuditTrail audit = new RecordingAuditTrail();
    public final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork()
            .enlist(listRepository, receiptRepository, accountRepository, events, audit);
    public final FixedFeatureFlags flags = new FixedFeatureFlags();
    public final RecordingProductAnalytics analytics = new RecordingProductAnalytics();
    public final RecordingEmailSender email = new RecordingEmailSender();
    public final RecordingPushSender push = new RecordingPushSender();

    public final ReceiptBookService receiptBook = new ReceiptBookService(receiptRepository);
    public final ListService lists = new ListService(listRepository, unitOfWork, events, ids, clock);
    public final ItemService items = new ItemService(listRepository, unitOfWork, events, ids, clock);
    public final FinishPurchaseService finishPurchase =
            new FinishPurchaseService(listRepository, receiptBook, unitOfWork, events, ids, clock);
    public final ReceiptReuseService receiptReuse =
            new ReceiptReuseService(listRepository, receiptBook, unitOfWork, events, ids, clock);
    public final ReceiptQueryService receipts = new ReceiptQueryService(receiptRepository);
    public final AccountService accounts = new AccountService(accountRepository, unitOfWork, ids, clock);
    public final CapabilityService capabilities = new CapabilityService(flags, analytics, ids, clock);
}
