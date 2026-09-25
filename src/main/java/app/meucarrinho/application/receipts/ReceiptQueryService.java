package app.meucarrinho.application.receipts;

import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReceiptQueryService implements GetReceipt, GetReceiptHistory {
    private final ReceiptRepository receipts;

    public ReceiptQueryService(ReceiptRepository receipts) {
        this.receipts = receipts;
    }

    @Override
    public Result<Receipt, ReceiptError> get(ReceiptId id, AccountId viewer) {
        return receipts.findById(id)
                .filter(receipt -> receipt.isVisibleTo(viewer))
                .<Result<Receipt, ReceiptError>>map(Result::ok)
                .orElseGet(() -> Result.err(new ReceiptError.ReceiptNotFound(id)));
    }

    @Override
    public List<ReceiptMonth> history(AccountId viewer, YearMonth from, YearMonth to, ZoneId zone) {
        Instant start = from.atDay(1).atStartOfDay(zone).toInstant();
        Instant end = to.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        Map<YearMonth, List<Receipt>> byMonth = new LinkedHashMap<>();
        for (Receipt receipt : receipts.findVisibleTo(viewer, start, end)) {
            YearMonth month = YearMonth.from(receipt.completedAt().atZone(zone));
            byMonth.computeIfAbsent(month, m -> new ArrayList<>()).add(receipt);
        }
        return byMonth.entrySet().stream()
                .map(e -> new ReceiptMonth(e.getKey(),
                        e.getValue().stream().map(Receipt::total).reduce(Money.ZERO, Money::plus), e.getValue()))
                .toList();
    }
}
