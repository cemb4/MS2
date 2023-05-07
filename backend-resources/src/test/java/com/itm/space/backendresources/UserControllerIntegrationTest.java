package com.itm.space.backendresources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.util.JsonUtil;
import com.itm.space.backendresources.worker.IdentityWorker;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerIntegrationTest extends BaseIntegrationTest {


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityWorker identityWorker;


    @Test
    @WithMockUser(username = "user", roles = {"MODERATOR"})
    public void testCreateUserOk() throws Exception {
        UserRequest userRequest = new UserRequest("username", "email@example.com", "password", "firstName", "lastName");
        doReturn("userId").when(identityWorker).createUser(any());
        //       verify(identityWorker, atLeast(1)).createUser(any());
        //then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(username = "user", roles = {"MODERATOR"})
    void testGetUserByIdOk() throws Exception {
        //give
        UUID id = UUID.randomUUID();
        String firstName = "John";
        String lastName = "Doe";
        String roleName = "ROLE_USER";
        String email = "johndoe@gmail.com";
        String groupName = "GROUP1";


        // stubs for IdentityWorker
        UserRepresentation userRepresentation = createUserRepresentation();
        List<GroupRepresentation> groupsRepresentation = createGroupsRepresentation(groupName);
        List<RoleRepresentation> roleRepresentation = createRolesRepresentation(roleName);

        doReturn(roleRepresentation).when(identityWorker).getUserRoles(id);
        doReturn(groupsRepresentation).when(identityWorker).getUserGroups(id);
        doReturn(userRepresentation).when(identityWorker).getUserById(id);


        //then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}", id))

                //assert
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.firstName").value(firstName))
                .andExpect(jsonPath("$.lastName").value(lastName))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value(roleName))
                .andExpect(jsonPath("$.groups[0]").value(groupName));

    }

    @Test
    public void testCreateUser_invalidRequest_shouldReturnBadRequest() throws Exception {
        UserRequest userRequest = new UserRequest(
                "",
                "",
                "",
                "",
                "");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(400));
    }

    @Test
    void testCreateUserUnauthorized() throws Exception {
        UserRequest userRequest = new UserRequest(
                "test",
                "test@example.com",
                "password",
                "John",
                "Doe"
        );
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(401));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testCreateUserUnauthorizedRoles() throws Exception {
        UserRequest userRequest = new UserRequest(
                "test",
                "test@example.com",
                "password",
                "John",
                "Doe"
        );
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(403));
    }


    @Test
    public void getUserById_UserUnauthorized() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/users" + "/" + randomId))
                .andExpect(status().is(401));
    }

    @Test
    @WithMockUser(username = "test", roles = "USER")
    public void getUserById_UserUnauthorizedRoles() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/users" + "/" + randomId))
                .andExpect(status().is(403));
    }

    @Test
    public void testGetUserByIdWithInvalidId() throws Exception {
        String invalidId = "invalid-id";
        mockMvc.perform(get("/api/users/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }


    @SneakyThrows
    // TODO add parameters
    private UserRepresentation createUserRepresentation() {
        return JsonUtil.getObjectFromJson("json/userRepresentation_correct.json", UserRepresentation.class);
    }

    private List<GroupRepresentation> createGroupsRepresentation(String nameOfGroup) {
        List<GroupRepresentation> groupsRepresentation = new ArrayList<>();
        var group = new GroupRepresentation();
        group.setName(nameOfGroup);
        groupsRepresentation.add(group);
        return groupsRepresentation;
    }

    private List<RoleRepresentation> createRolesRepresentation(String nameOfRole) {
        List<RoleRepresentation> rolesRepresentation = new ArrayList<>();
        var role = new RoleRepresentation();
        role.setName(nameOfRole);
        rolesRepresentation.add(role);
        return rolesRepresentation;
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
