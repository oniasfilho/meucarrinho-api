package br.com.oniasfilho.meucarrinho.session;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.dto.CreateShoppingSessionRequest;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionSummaryResponse;
import br.com.oniasfilho.meucarrinho.session.dto.UpdateShoppingSessionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class ShoppingSessionController {

    private final ShoppingSessionService service;

    public ShoppingSessionController(ShoppingSessionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShoppingSessionSummaryResponse> list(
        @RequestParam(required = false) ShoppingSessionStatus status
    ) {
        return service.list(status);
    }

    @PostMapping
    public ResponseEntity<ShoppingSessionDetailResponse> create(
        @Valid @RequestBody CreateShoppingSessionRequest request
    ) {
        ShoppingSessionDetailResponse response = service.create(request);
        return ResponseEntity
            .created(URI.create("/api/v1/sessions/" + response.id()))
            .body(response);
    }

    @GetMapping("/{sessionId}")
    public ShoppingSessionDetailResponse getDetail(@PathVariable UUID sessionId) {
        return service.getDetail(sessionId);
    }

    @PatchMapping("/{sessionId}")
    public ShoppingSessionDetailResponse update(
        @PathVariable UUID sessionId,
        @Valid @RequestBody UpdateShoppingSessionRequest request
    ) {
        return service.update(sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID sessionId) {
        service.delete(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/complete")
    public ShoppingSessionDetailResponse complete(@PathVariable UUID sessionId) {
        return service.complete(sessionId);
    }

    @PostMapping("/{sessionId}/duplicate")
    public ResponseEntity<ShoppingSessionDetailResponse> duplicate(@PathVariable UUID sessionId) {
        ShoppingSessionDetailResponse response = service.duplicate(sessionId);
        return ResponseEntity
            .created(URI.create("/api/v1/sessions/" + response.id()))
            .body(response);
    }
}

