package app.meucarrinho.api.rest.receipts;

import app.meucarrinho.api.rest.MoneyDto;
import java.util.List;

public record MonthTotalsResponse(List<MonthTotal> months) {
    public record MonthTotal(String month, MoneyDto total, int receiptCount) {}
}
