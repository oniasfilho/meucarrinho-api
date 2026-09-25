package app.meucarrinho.application.receipts;

import app.meucarrinho.domain.shared.AccountId;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

public interface GetReceiptHistory {
    ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    List<ReceiptMonth> history(AccountId viewer, YearMonth from, YearMonth to, ZoneId zone);
}
