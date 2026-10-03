package app.meucarrinho.api.rest.receipts;

import app.meucarrinho.api.rest.ActorDto;
import app.meucarrinho.api.rest.Fields;
import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.QuantityDto;
import app.meucarrinho.application.receipts.ReceiptMonth;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.StoreName;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/** Translates receipts and history months into DTOs, newest first. */
public final class ReceiptMapper {
    private ReceiptMapper() {}

    public static ReceiptId receiptId(UUID id) {
        return Fields.id("The receipt ID", () -> new ReceiptId(id));
    }

    public static ResponseEntity<ReceiptResponse> created(Receipt receipt) {
        return ResponseEntity.created(URI.create("/v1/receipts/" + receipt.id())).body(response(receipt));
    }

    public static ReceiptResponse response(Receipt receipt) {
        return new ReceiptResponse(
                receipt.id().value(),
                receipt.listId().value(),
                receipt.name().value(),
                receipt.store().map(StoreName::value).orElse(null),
                receipt.completedAt(),
                ActorDto.of(receipt.finishedBy()),
                receipt.participants().stream().map(ActorDto::of).toList(),
                receipt.lines().stream()
                        .map(line -> new ReceiptLineResponse(line.name().value(), QuantityDto.of(line.quantity()),
                                MoneyDto.ofNullable(line.unitPrice()), MoneyDto.of(line.subtotal())))
                        .toList(),
                MoneyDto.of(receipt.total()),
                receipt.budget().map(budget -> MoneyDto.of(budget.amount())).orElse(null),
                MoneyDto.ofNullable(receipt.budgetDelta()),
                receipt.overBudget());
    }

    static List<ReceiptPageResponse.Month> months(List<ReceiptMonth> months) {
        return newestFirst(months).stream()
                .map(month -> new ReceiptPageResponse.Month(month.month().toString(), MoneyDto.of(month.total()),
                        month.receipts().stream()
                                .sorted(Comparator.comparing(Receipt::completedAt).reversed())
                                .map(ReceiptMapper::summary)
                                .toList()))
                .toList();
    }

    static List<MonthTotalsResponse.MonthTotal> totals(List<ReceiptMonth> months) {
        return newestFirst(months).stream()
                .map(month -> new MonthTotalsResponse.MonthTotal(month.month().toString(), MoneyDto.of(month.total()),
                        month.receipts().size()))
                .toList();
    }

    private static List<ReceiptMonth> newestFirst(List<ReceiptMonth> months) {
        return months.stream().sorted(Comparator.comparing(ReceiptMonth::month).reversed()).toList();
    }

    private static ReceiptPageResponse.Summary summary(Receipt receipt) {
        return new ReceiptPageResponse.Summary(receipt.id().value(), receipt.name().value(),
                receipt.store().map(StoreName::value).orElse(null), receipt.completedAt(),
                MoneyDto.of(receipt.total()), receipt.lines().size(), receipt.overBudget());
    }
}
