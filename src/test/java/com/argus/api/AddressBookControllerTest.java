package com.argus.api;

import com.argus.api.dto.LabelRequest;
import com.argus.core.exception.GlobalExceptionHandler;
import com.argus.core.security.AuthContext;
import com.argus.core.security.AuthenticatedUser;
import com.argus.domain.model.AddressLabel;
import com.argus.domain.model.User;
import com.argus.domain.service.AddressBookService;
import com.argus.domain.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressBookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AddressBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressBookService addressBookService;

    @MockBean
    private UserService userService;

    private MockedStatic<AuthContext> mockedAuthContext;
    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final String TEST_SUB = "test-sub";
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        mockedAuthContext = mockStatic(AuthContext.class);
        AuthenticatedUser authUser = new AuthenticatedUser(TEST_SUB, TEST_EMAIL, "USER");
        mockedAuthContext.when(AuthContext::currentUser).thenReturn(authUser);

        User user = User.builder().id(TEST_USER_ID).email(TEST_EMAIL).supabaseUid(TEST_SUB).build();
        when(userService.getOrCreateUser(eq(TEST_SUB), eq(TEST_EMAIL))).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        mockedAuthContext.close();
    }

    @Test
    void addLabel_shouldReturn201() throws Exception {
        LabelRequest request = new LabelRequest();
        request.setAddress("0x1234567890123456789012345678901234567890");
        request.setLabel("Test Label");
        request.setCategory("whale");

        AddressLabel created = AddressLabel.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .address(request.getAddress())
                .label(request.getLabel())
                .category(request.getCategory())
                .build();

        when(addressBookService.addLabel(eq(request.getAddress()), eq(request.getLabel()), eq(request.getCategory()),
                eq(TEST_USER_ID)))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/address-book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").value(request.getAddress()))
                .andExpect(jsonPath("$.labels[0].label").value(request.getLabel()));
    }

    @Test
    void getLabels_shouldReturn200() throws Exception {
        String address = "0x1234567890123456789012345678901234567890";
        List<AddressLabel> labels = List.of(
                AddressLabel.builder().id(1L).userId(TEST_USER_ID).address(address).label("L1").build());

        when(addressBookService.getLabels(eq(address), eq(TEST_USER_ID))).thenReturn(labels);

        mockMvc.perform(get("/api/v1/address-book/{address}", address))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(address))
                .andExpect(jsonPath("$.labels[0].label").value("L1"));
    }
}
