package br.com.oniasfilho.meucarrinho.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.BusinessRuleException;
import br.com.oniasfilho.meucarrinho.common.error.GlobalExceptionHandler;
import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.session.dto.ShoppingSessionDetailResponse;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemController;
import br.com.oniasfilho.meucarrinho.session.item.ShoppingSessionItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ShoppingSessionController.class, ShoppingSessionItemController.class})
@Import(GlobalExceptionHandler.class)
class ShoppingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShoppingSessionService sessionService;

    @MockitoBean
    private ShoppingSessionItemService itemService;

    @Test
    void createsSessionWith201AndLocation() throws Exception {
        ShoppingSessionDetailResponse response = detail(ShoppingSessionStatus.ACTIVE);
        when(sessionService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Compras 01/09",
                      "storeName": null,
                      "budget": 250.00
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/sessions/" + response.id()))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getsSessionDetail() throws Exception {
        ShoppingSessionDetailResponse response = detail(ShoppingSessionStatus.ACTIVE);
        when(sessionService.getDetail(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/v1/sessions/{id}", response.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id().toString()))
            .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void mapsUnknownSessionToProblemDetail404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(sessionService.getDetail(unknownId))
            .thenThrow(new ResourceNotFoundException("Shopping session was not found."));

        mockMvc.perform(get("/api/v1/sessions/{id}", unknownId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.detail").value("Shopping session was not found."));
    }

    @Test
    void rejectsQuantityZeroBeforeCallingService() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(patch(
                "/api/v1/sessions/{sessionId}/items/{itemId}/quantity",
                sessionId,
                itemId
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": 0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"))
            .andExpect(jsonPath("$.errors.quantity").exists());

        verify(itemService, never()).updateQuantity(any(), any(), any());
    }

    @Test
    void mapsSecondCompletionTo409() throws Exception {
        ShoppingSessionDetailResponse completed = detail(ShoppingSessionStatus.COMPLETED);
        when(sessionService.complete(completed.id()))
            .thenReturn(completed)
            .thenThrow(new BusinessRuleException("Completed shopping sessions cannot be modified."));

        mockMvc.perform(post("/api/v1/sessions/{id}/complete", completed.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/sessions/{id}/complete", completed.id()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Business rule violation"));
    }

    private ShoppingSessionDetailResponse detail(ShoppingSessionStatus status) {
        Instant completedAt = status == ShoppingSessionStatus.COMPLETED
            ? Instant.parse("2026-09-01T04:00:00Z")
            : null;
        return new ShoppingSessionDetailResponse(
            UUID.randomUUID(),
            "Compra",
            null,
            new BigDecimal("250.00"),
            status,
            0,
            new BigDecimal("0.00"),
            new BigDecimal("250.00"),
            false,
            Instant.parse("2026-09-01T03:00:00Z"),
            completedAt,
            List.of()
        );
    }
}
