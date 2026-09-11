package br.com.oniasfilho.meucarrinho.session.item;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.common.error.GlobalExceptionHandler;
import br.com.oniasfilho.meucarrinho.common.error.ResourceNotFoundException;
import br.com.oniasfilho.meucarrinho.session.item.dto.ItemLabelPhotoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemLabelPhotoController.class)
@Import(GlobalExceptionHandler.class)
class ItemLabelPhotoControllerTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/sessions/{s}/items/{i}/label-photo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemLabelPhotoService service;

    @Test
    void uploadsMultipartAndReturnsKeyWithPresignedUrl() throws Exception {
        ItemLabelPhotoResponse response = new ItemLabelPhotoResponse(
            "sessions/x/items/y/abc.png",
            "http://localhost:4566/test-media/sessions/x/items/y/abc.png?X-Amz-Signature=stub",
            Instant.parse("2026-09-10T12:15:00Z")
        );
        when(service.upload(eq(SESSION_ID), eq(ITEM_ID), any())).thenReturn(response);

        mockMvc.perform(multipart(PATH, SESSION_ID, ITEM_ID)
                .file(new MockMultipartFile("file", "label.png", "image/png", new byte[] {1, 2, 3})))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labelPhotoKey").value("sessions/x/items/y/abc.png"))
            .andExpect(jsonPath("$.labelPhotoUrl").value(response.labelPhotoUrl()));
    }

    @Test
    void getReturnsPresignedUrl() throws Exception {
        ItemLabelPhotoResponse response = new ItemLabelPhotoResponse(
            "sessions/x/items/y/abc.png", "http://localhost:4566/signed", Instant.now()
        );
        when(service.get(SESSION_ID, ITEM_ID)).thenReturn(response);

        mockMvc.perform(get(PATH, SESSION_ID, ITEM_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labelPhotoUrl").value("http://localhost:4566/signed"));
    }

    @Test
    void getReturns404WhenItemHasNoPhoto() throws Exception {
        when(service.get(SESSION_ID, ITEM_ID))
            .thenThrow(new ResourceNotFoundException("This item has no label photo."));

        mockMvc.perform(get(PATH, SESSION_ID, ITEM_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.detail").value("This item has no label photo."));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete(PATH, SESSION_ID, ITEM_ID))
            .andExpect(status().isNoContent());

        verify(service).delete(SESSION_ID, ITEM_ID);
    }
}
