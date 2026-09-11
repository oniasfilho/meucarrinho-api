package br.com.oniasfilho.meucarrinho.user;

import java.math.BigDecimal;

import br.com.oniasfilho.meucarrinho.session.ShoppingSessionRepository;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItem;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemRepository;
import br.com.oniasfilho.meucarrinho.user.dto.AccountStatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountStatsService {

    private final CurrentUserService currentUserService;
    private final ShoppingSessionRepository sessionRepository;
    private final ShoppingSessionItemRepository itemRepository;

    public AccountStatsService(
        CurrentUserService currentUserService,
        ShoppingSessionRepository sessionRepository,
        ShoppingSessionItemRepository itemRepository
    ) {
        this.currentUserService = currentUserService;
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public AccountStatsResponse getStats() {
        AppUser user = currentUserService.getCurrentUser();
        long sessionCount = sessionRepository.countByUser_Id(user.getId());
        long completedSessionCount = sessionRepository.countByUser_IdAndStatus(
            user.getId(),
            ShoppingSessionStatus.COMPLETED
        );
        BigDecimal totalRecordedSpending = itemRepository
            .findAllByUserIdAndSessionStatus(user.getId(), ShoppingSessionStatus.COMPLETED)
            .stream()
            .map(this::subtotal)
            .reduce(new BigDecimal("0.00"), BigDecimal::add);

        return new AccountStatsResponse(sessionCount, completedSessionCount, totalRecordedSpending);
    }

    private BigDecimal subtotal(ShoppingSessionItem item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}

