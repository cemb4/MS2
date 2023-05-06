package com.itm.space.backendresources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 9000)

public class UserControllerIntegrationTest extends BaseIntegrationTest {


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private UserService userService;

    private UserRequest userRequest;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userRequest = new UserRequest(
                "test",
                "test@example.com",
                "password",
                "John",
                "Doe"
        );

    }
    @Test
    @WithMockUser(username = "user", roles = {"MODERATOR"})
    public void testCreateUserOk() throws Exception {
        UserRequest userRequest = new UserRequest("username", "email@example.com", "password", "firstName", "lastName");
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:9000/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(username = "user", roles = {"MODERATOR"})
    void testGetUserByIdOk() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse userResponse = new UserResponse("John", "Doe", "johndoe@gmail.com", Collections.singletonList("ROLE_USER"), Collections.singletonList("GROUP1"));
        when(userService.getUserById(id)).thenReturn(userResponse);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:9000/api/users/{id}"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.firstName").value(userResponse.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(userResponse.getLastName()))
                .andExpect(jsonPath("$.email").value(userResponse.getEmail()))
                .andExpect(jsonPath("$.roles[0]").value(userResponse.getRoles().get(0)))
                .andExpect(jsonPath("$.groups[0]").value(userResponse.getGroups().get(0)));

    }

    @Test
    public void testCreateUser_invalidRequest_shouldReturnBadRequest() throws Exception {
        UserRequest userRequest = new UserRequest("", "", "", "", "");
        mockMvc.perform(post("http://localhost:9000/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(400));
    }

    @Test
    void testCreateUserUnauthorized() throws Exception {
        mockMvc.perform(post("http://localhost:9000/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(401));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testCreateUserUnauthorizedRoles() throws Exception {
        mockMvc.perform(post("http://localhost:9000/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(403));
    }


    @Test
    public void getUserById_UserUnauthorized() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("http://localhost:9000/api/users" + "/" + randomId))
                .andExpect(status().is(401));
    }

    @Test
    @WithMockUser(username = "test", roles = "USER")
    public void getUserById_UserUnauthorizedRoles() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("http://localhost:9000/api/users" + "/" + randomId))
                .andExpect(status().is(403));
    }

    @Test
    public void testGetUserByIdWithInvalidId() throws Exception {
        String invalidId = "invalid-id";
        mockMvc.perform(get("http://localhost:9000/api/users/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

//    @Test
//    @WithMockUser(username = "user", roles = {"MODERATOR"})
//    public void testGetUserByIdWithNonExistingId() throws Exception {
//        UUID id = UUID.randomUUID();
//        when(userService.getUserById(id)).thenThrow(new BackendResourcesException("Not found", HttpStatus.NOT_FOUND));
//        mockMvc.perform(get("/api/users/{id}", id.toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().is(404));
//    }


}
