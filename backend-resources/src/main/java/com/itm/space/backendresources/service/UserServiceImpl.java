package com.itm.space.backendresources.service;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.mapper.UserMapper;
import com.itm.space.backendresources.worker.IdentityWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final IdentityWorker identityWorker;
    private final UserMapper userMapper;

    public void createUser(UserRequest userRequest) {
        CredentialRepresentation password = preparePasswordRepresentation(userRequest.getPassword());
        UserRepresentation user = prepareUserRepresentation(userRequest, password);
        try {
            String userId = identityWorker.createUser(user);
            log.info("Created UserId: {}", userId);
        } catch (WebApplicationException ex) { log.error("Exception on \"createUser\": ", ex);
            throw new BackendResourcesException(ex.getMessage(), HttpStatus.resolve(ex.getResponse().getStatus()));
        }
    }

    @Override
    public UserResponse getUserById(UUID id) {
        UserRepresentation userRepresentation = identityWorker.getUserById(id);
        List<RoleRepresentation> userRoles = identityWorker.getUserRoles(id);
        List<GroupRepresentation> userGroups = identityWorker.getUserGroups(id);
        try {

        } catch (RuntimeException ex) {
            log.error("Exception on \"getUserById\": ", ex);
            throw new BackendResourcesException(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return userMapper.userRepresentationToUserResponse(userRepresentation, userRoles, userGroups);
    }


    private CredentialRepresentation preparePasswordRepresentation(String password) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(password);
        return credentialRepresentation;
    }

    private UserRepresentation prepareUserRepresentation(UserRequest userRequest,
                                                         CredentialRepresentation credentialRepresentation) {
        UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername(userRequest.getUsername());
        newUser.setEmail(userRequest.getEmail());
        newUser.setCredentials(List.of(credentialRepresentation));
        newUser.setEnabled(true);
        newUser.setFirstName(userRequest.getFirstName());
        newUser.setLastName(userRequest.getLastName());
        return newUser;
    }
}
