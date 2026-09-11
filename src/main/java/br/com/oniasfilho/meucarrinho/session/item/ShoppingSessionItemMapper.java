package br.com.oniasfilho.meucarrinho.session.item;

import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService;
import br.com.oniasfilho.meucarrinho.common.storage.ObjectStorageService.PresignedUrl;
import br.com.oniasfilho.meucarrinho.session.item.dto.ShoppingSessionItemResponse;
import org.springframework.stereotype.Component;

@Component
public class ShoppingSessionItemMapper {

    private final ObjectStorageService objectStorage;

    public ShoppingSessionItemMapper(ObjectStorageService objectStorage) {
        this.objectStorage = objectStorage;
    }

    public ShoppingSessionItemResponse toResponse(ShoppingSessionItem item) {
        String labelPhotoKey = item.getLabelPhotoKey();
        String labelPhotoUrl = objectStorage.presignedGet(labelPhotoKey)
            .map(PresignedUrl::url)
            .orElse(null);
        return new ShoppingSessionItemResponse(
            item.getId(),
            item.getName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getNote(),
            labelPhotoKey,
            labelPhotoUrl,
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
