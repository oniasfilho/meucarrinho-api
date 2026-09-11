package br.com.oniasfilho.meucarrinho.session.item;

import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.BusinessRuleException;
import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService;
import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService.PresignedUrl;
import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService.StoredObject;
import br.com.oniasfilho.meucarrinho.session.ShoppingSession;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionRepository;
import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;
import br.com.oniasfilho.meucarrinho.session.item.dto.ItemLabelPhotoResponse;
import br.com.oniasfilho.meucarrinho.user.AppUser;
import br.com.oniasfilho.meucarrinho.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owns the item label photo lifecycle: upload to object storage, expose a presigned URL,
 * delete. Kept separate from {@link ShoppingSessionItemService} so that service's shape
 * (and its unit tests) stay unaware of object storage.
 */
@Service
public class ItemLabelPhotoService {

    private final ShoppingSessionRepository sessionRepository;
    private final ShoppingSessionItemRepository itemRepository;
    private final CurrentUserService currentUserService;
    private final ObjectStorageService objectStorage;

    public ItemLabelPhotoService(
        ShoppingSessionRepository sessionRepository,
        ShoppingSessionItemRepository itemRepository,
        CurrentUserService currentUserService,
        ObjectStorageService objectStorage
    ) {
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
        this.currentUserService = currentUserService;
        this.objectStorage = objectStorage;
    }

    @Transactional
    public ItemLabelPhotoResponse upload(UUID sessionId, UUID itemId, MultipartFile file) {
        findOwnedSession(sessionId, true);
        ShoppingSessionItem item = findItem(itemId, sessionId);

        String previousKey = item.getLabelPhotoKey();
        StoredObject stored = objectStorage.store(objectKeyPrefix(sessionId, itemId), file);
        item.assignLabelPhotoKey(stored.key());
        itemRepository.saveAndFlush(item);

        if (previousKey != null && !previousKey.equals(stored.key())) {
            objectStorage.delete(previousKey);
        }
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public ItemLabelPhotoResponse get(UUID sessionId, UUID itemId) {
        findOwnedSession(sessionId, false);
        ShoppingSessionItem item = findItem(itemId, sessionId);
        if (item.getLabelPhotoKey() == null) {
            throw new ResourceNotFoundException("This item has no label photo.");
        }
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID sessionId, UUID itemId) {
        findOwnedSession(sessionId, true);
        ShoppingSessionItem item = findItem(itemId, sessionId);
        String key = item.getLabelPhotoKey();
        if (key == null) {
            return;
        }
        item.assignLabelPhotoKey(null);
        itemRepository.saveAndFlush(item);
        objectStorage.delete(key);
    }

    private ItemLabelPhotoResponse toResponse(ShoppingSessionItem item) {
        String key = item.getLabelPhotoKey();
        PresignedUrl presigned = objectStorage.presignedGet(key).orElse(null);
        return new ItemLabelPhotoResponse(
            key,
            presigned == null ? null : presigned.url(),
            presigned == null ? null : presigned.expiresAt()
        );
    }

    private ShoppingSession findOwnedSession(UUID sessionId, boolean mustBeActive) {
        AppUser user = currentUserService.getCurrentUser();
        ShoppingSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Shopping session was not found."));
        if (mustBeActive && session.getStatus() != ShoppingSessionStatus.ACTIVE) {
            throw new BusinessRuleException("Items in completed shopping sessions cannot be changed.");
        }
        return session;
    }

    private ShoppingSessionItem findItem(UUID itemId, UUID sessionId) {
        return itemRepository.findByIdAndSessionId(itemId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping session item was not found."));
    }

    private static String objectKeyPrefix(UUID sessionId, UUID itemId) {
        return "sessions/" + sessionId + "/items/" + itemId;
    }
}
