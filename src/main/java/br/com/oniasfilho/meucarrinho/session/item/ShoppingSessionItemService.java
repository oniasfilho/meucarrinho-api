package br.com.oniasfilho.meucarrinho.session.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.BusinessRuleException;
import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.session.ShoppingSession;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionMapper;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionRepository;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.dto.CreateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.UpdateItemQuantityRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.UpdateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import br.com.oniasfilho.meucarrinho.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingSessionItemService {

    private final ShoppingSessionRepository sessionRepository;
    private final ShoppingSessionItemRepository itemRepository;
    private final CurrentUserService currentUserService;
    private final ShoppingSessionMapper sessionMapper;

    public ShoppingSessionItemService(
        ShoppingSessionRepository sessionRepository,
        ShoppingSessionItemRepository itemRepository,
        CurrentUserService currentUserService,
        ShoppingSessionMapper sessionMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
        this.currentUserService = currentUserService;
        this.sessionMapper = sessionMapper;
    }

    @Transactional
    public ShoppingSessionDetailResponse create(UUID sessionId, CreateShoppingSessionItemRequest request) {
        ShoppingSession session = findOwnedActiveSession(sessionId);
        String name = requiredText(request.name());
        validatePrice(request.unitPrice());
        validateQuantity(request.quantity());

        ShoppingSessionItem item = new ShoppingSessionItem(
            UUID.randomUUID(),
            session,
            name,
            request.unitPrice(),
            request.quantity(),
            request.note(),
            null
        );
        itemRepository.save(item);
        return toDetail(session);
    }

    @Transactional
    public ShoppingSessionDetailResponse update(
        UUID sessionId,
        UUID itemId,
        UpdateShoppingSessionItemRequest request
    ) {
        ShoppingSession session = findOwnedActiveSession(sessionId);
        ShoppingSessionItem item = findItem(itemId, sessionId);

        String name = request.name() == null ? item.getName() : requiredText(request.name());
        BigDecimal unitPrice = request.unitPrice() == null ? item.getUnitPrice() : request.unitPrice();
        int quantity = request.quantity() == null ? item.getQuantity() : request.quantity();
        String note = request.note() == null ? item.getNote() : request.note();

        validatePrice(unitPrice);
        validateQuantity(quantity);
        item.update(name, unitPrice, quantity, note);
        itemRepository.saveAndFlush(item);
        return toDetail(session);
    }

    @Transactional
    public ShoppingSessionDetailResponse updateQuantity(
        UUID sessionId,
        UUID itemId,
        UpdateItemQuantityRequest request
    ) {
        ShoppingSession session = findOwnedActiveSession(sessionId);
        ShoppingSessionItem item = findItem(itemId, sessionId);
        validateQuantity(request.quantity());

        item.updateQuantity(request.quantity());
        itemRepository.saveAndFlush(item);
        return toDetail(session);
    }

    @Transactional
    public void delete(UUID sessionId, UUID itemId) {
        findOwnedActiveSession(sessionId);
        ShoppingSessionItem item = findItem(itemId, sessionId);
        itemRepository.delete(item);
    }

    private ShoppingSessionDetailResponse toDetail(ShoppingSession session) {
        List<ShoppingSessionItem> items = itemRepository
            .findAllBySessionIdOrderByCreatedAtDesc(session.getId());
        return sessionMapper.toDetail(session, items);
    }

    private ShoppingSession findOwnedActiveSession(UUID sessionId) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Shopping session was not found."));
        if (session.getStatus() != ShoppingSessionStatus.ACTIVE) {
            throw new BusinessRuleException("Items in completed shopping sessions cannot be changed.");
        }
        return session;
    }

    private ShoppingSessionItem findItem(UUID itemId, UUID sessionId) {
        return itemRepository.findByIdAndSessionId(itemId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping session item was not found."));
    }

    private String requiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Item name must not be blank.");
        }
        return value.trim();
    }

    private void validatePrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price must not be negative.");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
    }
}

