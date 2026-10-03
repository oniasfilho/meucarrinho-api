package app.meucarrinho.api.rest.receipts;

import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.QuantityDto;
import org.jspecify.annotations.Nullable;

public record ReceiptLineResponse(String name, QuantityDto quantity, @Nullable MoneyDto unitPrice, MoneyDto subtotal) {}
