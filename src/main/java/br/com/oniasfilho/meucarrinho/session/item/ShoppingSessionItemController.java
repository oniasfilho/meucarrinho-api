package br.com.oniasfilho.meucarrinho.session.item;

import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.dto.CreateShoppingSessionItemRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.UpdateItemQuantityRequest;
import br.com.oniasfilho.meucarrinho.session.item.dto.UpdateShoppingSessionItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/items")
public class ShoppingSessionItemController {

    private final ShoppingSessionItemService service;

    public ShoppingSessionItemController(ShoppingSessionItemService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShoppingSessionDetailResponse> create(
        @PathVariable UUID sessionId,
        @Valid @RequestBody CreateShoppingSessionItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(sessionId, request));
    }

    @PatchMapping("/{itemId}")
    public ShoppingSessionDetailResponse update(
        @PathVariable UUID sessionId,
        @PathVariable UUID itemId,
        @Valid @RequestBody UpdateShoppingSessionItemRequest request
    ) {
        return service.update(sessionId, itemId, request);
    }

    @PatchMapping("/{itemId}/quantity")
    public ShoppingSessionDetailResponse updateQuantity(
        @PathVariable UUID sessionId,
        @PathVariable UUID itemId,
        @Valid @RequestBody UpdateItemQuantityRequest request
    ) {
        return service.updateQuantity(sessionId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID sessionId,
        @PathVariable UUID itemId
    ) {
        service.delete(sessionId, itemId);
        return ResponseEntity.noContent().build();
    }
}

