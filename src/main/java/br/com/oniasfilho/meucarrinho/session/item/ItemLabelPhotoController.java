package br.com.oniasfilho.meucarrinho.session.item;

import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.item.dto.ItemLabelPhotoResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/items/{itemId}/label-photo")
public class ItemLabelPhotoController {

    private final ItemLabelPhotoService service;

    public ItemLabelPhotoController(ItemLabelPhotoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemLabelPhotoResponse upload(
        @PathVariable UUID sessionId,
        @PathVariable UUID itemId,
        @RequestPart("file") MultipartFile file
    ) {
        return service.upload(sessionId, itemId, file);
    }

    @GetMapping
    public ItemLabelPhotoResponse get(
        @PathVariable UUID sessionId,
        @PathVariable UUID itemId
    ) {
        return service.get(sessionId, itemId);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
        @PathVariable UUID sessionId,
        @PathVariable UUID itemId
    ) {
        service.delete(sessionId, itemId);
        return ResponseEntity.noContent().build();
    }
}
