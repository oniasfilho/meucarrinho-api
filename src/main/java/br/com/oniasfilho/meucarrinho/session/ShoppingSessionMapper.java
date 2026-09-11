package br.com.oniasfilho.meucarrinho.session;

import java.math.BigDecimal;
import java.util.List;

import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionSummaryResponse;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItem;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemMapper;
import org.springframework.stereotype.Component;

@Component
public class ShoppingSessionMapper {

    private final ShoppingSessionItemMapper itemMapper;

    public ShoppingSessionMapper(ShoppingSessionItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    public ShoppingSessionSummaryResponse toSummary(
        ShoppingSession session,
        List<ShoppingSessionItem> items
    ) {
        CalculatedValues values = calculate(session, items);
        return new ShoppingSessionSummaryResponse(
            session.getId(),
            session.getName(),
            session.getStoreName(),
            session.getBudget(),
            session.getStatus(),
            values.itemCount(),
            values.total(),
            values.remainingBudget(),
            values.overBudget(),
            session.getCreatedAt(),
            session.getCompletedAt()
        );
    }

    public ShoppingSessionDetailResponse toDetail(
        ShoppingSession session,
        List<ShoppingSessionItem> items
    ) {
        CalculatedValues values = calculate(session, items);
        return new ShoppingSessionDetailResponse(
            session.getId(),
            session.getName(),
            session.getStoreName(),
            session.getBudget(),
            session.getStatus(),
            values.itemCount(),
            values.total(),
            values.remainingBudget(),
            values.overBudget(),
            session.getCreatedAt(),
            session.getCompletedAt(),
            items.stream().map(itemMapper::toResponse).toList()
        );
    }

    private CalculatedValues calculate(ShoppingSession session, List<ShoppingSessionItem> items) {
        int itemCount = items.stream()
            .mapToInt(ShoppingSessionItem::getQuantity)
            .sum();

        BigDecimal total = items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(new BigDecimal("0.00"), BigDecimal::add);

        BigDecimal remainingBudget = session.getBudget() == null
            ? null
            : session.getBudget().subtract(total);
        boolean overBudget = session.getBudget() != null && total.compareTo(session.getBudget()) > 0;

        return new CalculatedValues(itemCount, total, remainingBudget, overBudget);
    }

    private record CalculatedValues(
        int itemCount,
        BigDecimal total,
        BigDecimal remainingBudget,
        boolean overBudget
    ) {
    }
}
