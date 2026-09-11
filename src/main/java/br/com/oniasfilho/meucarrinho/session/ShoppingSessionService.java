package br.com.oniasfilho.meucarrinho.session;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import br.com.oniasfilho.meucarrinho.common.error.BusinessRuleException;
import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.session.dto.CreateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionSummaryResponse;
import br.com.oniasfilho.meucarrinho.session.dto.UpdateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItem;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemRepository;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import br.com.oniasfilho.meucarrinho.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingSessionService {

    private static final int SESSION_NAME_MAX_LENGTH = 120;
    private static final String COPY_SUFFIX = " (cópia)";

    private final ShoppingSessionRepository sessionRepository;
    private final ShoppingSessionItemRepository itemRepository;
    private final CurrentUserService currentUserService;
    private final ShoppingSessionMapper mapper;
    private final Clock clock;

    public ShoppingSessionService(
        ShoppingSessionRepository sessionRepository,
        ShoppingSessionItemRepository itemRepository,
        CurrentUserService currentUserService,
        ShoppingSessionMapper mapper,
        Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ShoppingSessionSummaryResponse> list(ShoppingSessionStatus status) {
        AppUser user = currentUserService.getCurrentUser();
        List<ShoppingSession> sessions = status == null
            ? sessionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
            : sessionRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);

        if (sessions.isEmpty()) {
            return List.of();
        }

        List<UUID> sessionIds = sessions.stream().map(ShoppingSession::getId).toList();
        Map<UUID, List<ShoppingSessionItem>> itemsBySession = itemRepository
            .findAllBySessionIdInOrderByCreatedAtDesc(sessionIds)
            .stream()
            .collect(Collectors.groupingBy(item -> item.getSession().getId()));

        return sessions.stream()
            .map(session -> mapper.toSummary(
                session,
                itemsBySession.getOrDefault(session.getId(), Collections.emptyList())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public ShoppingSessionDetailResponse getDetail(UUID sessionId) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession session = findOwnedSession(sessionId, user.getId());
        return toDetail(session);
    }

    @Transactional
    public ShoppingSessionDetailResponse create(CreateShoppingSessionRequest request) {
        AppUser user = currentUserService.getCurrentUser();
        String name = requiredText(request.name(), "Session name");
        String storeName = optionalText(request.storeName());
        validateBudget(request.budget());

        ShoppingSession session = new ShoppingSession(
            UUID.randomUUID(),
            user,
            name,
            storeName,
            request.budget()
        );
        sessionRepository.save(session);
        return mapper.toDetail(session, List.of());
    }

    @Transactional
    public ShoppingSessionDetailResponse update(UUID sessionId, UpdateShoppingSessionRequest request) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession session = findOwnedSession(sessionId, user.getId());
        requireActive(session);

        String name = request.name() == null
            ? session.getName()
            : requiredText(request.name(), "Session name");
        String storeName = request.storeName() == null
            ? session.getStoreName()
            : optionalText(request.storeName());
        BigDecimal budget = request.budget() == null ? session.getBudget() : request.budget();
        validateBudget(budget);

        session.updateSettings(name, storeName, budget);
        sessionRepository.saveAndFlush(session);
        return toDetail(session);
    }

    @Transactional
    public void delete(UUID sessionId) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession session = findOwnedSession(sessionId, user.getId());
        sessionRepository.delete(session);
    }

    @Transactional
    public ShoppingSessionDetailResponse complete(UUID sessionId) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession session = findOwnedSession(sessionId, user.getId());
        requireActive(session);

        session.complete(Instant.now(clock));
        sessionRepository.saveAndFlush(session);
        return toDetail(session);
    }

    @Transactional
    public ShoppingSessionDetailResponse duplicate(UUID sessionId) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession original = findOwnedSession(sessionId, user.getId());
        List<ShoppingSessionItem> originalItems = itemRepository
            .findAllBySessionIdOrderByCreatedAtDesc(original.getId());

        ShoppingSession copy = new ShoppingSession(
            UUID.randomUUID(),
            user,
            copyName(original.getName()),
            original.getStoreName(),
            original.getBudget()
        );
        sessionRepository.save(copy);

        List<ShoppingSessionItem> copiedItems = originalItems.stream()
            .map(item -> new ShoppingSessionItem(
                UUID.randomUUID(),
                copy,
                item.getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getNote(),
                item.getLabelPhotoKey()
            ))
            .toList();
        itemRepository.saveAll(copiedItems);

        return mapper.toDetail(copy, copiedItems);
    }

    private ShoppingSessionDetailResponse toDetail(ShoppingSession session) {
        List<ShoppingSessionItem> items = itemRepository
            .findAllBySessionIdOrderByCreatedAtDesc(session.getId());
        return mapper.toDetail(session, items);
    }

    private ShoppingSession findOwnedSession(UUID sessionId, UUID userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping session was not found."));
    }

    private void requireActive(ShoppingSession session) {
        if (session.getStatus() != ShoppingSessionStatus.ACTIVE) {
            throw new BusinessRuleException("Completed shopping sessions cannot be modified.");
        }
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateBudget(BigDecimal budget) {
        if (budget != null && budget.signum() < 0) {
            throw new IllegalArgumentException("Budget must not be negative.");
        }
    }

    private String copyName(String originalName) {
        int maximumOriginalLength = SESSION_NAME_MAX_LENGTH - COPY_SUFFIX.length();
        String baseName = originalName.length() <= maximumOriginalLength
            ? originalName
            : originalName.substring(0, maximumOriginalLength).stripTrailing();
        return baseName + COPY_SUFFIX;
    }
}
